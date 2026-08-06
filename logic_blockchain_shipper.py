import json
import sqlite3
import hashlib
import urllib.error
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

from topology_sidecar import DB_PATH, LOGIC_SHIPPER_PORT, migrate, queue_logic_payload

CLOUDFLARE_URL_FILE = Path(r"C:\Users\viper\VIPER_JAVA_RISC\LOGIC_SHIPPER_CLOUDFLARE_URL.txt")


def send_json(handler, status, payload):
    """Send json.

    Args: handler, status, payload.
    """
    body = json.dumps(payload, indent=2).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


def get_body(handler):
    """Get body.

    Args: handler.
    """
    length = int(handler.headers.get("Content-Length", "0"))
    if length <= 0:
        return {}
    return json.loads(handler.rfile.read(length).decode("utf-8"))


def get_raw_body(handler):
    """Get raw body.

    Args: handler.
    """
    length = int(handler.headers.get("Content-Length", "0"))
    if length <= 0:
        return b""
    return handler.rfile.read(length)


def sha256_text(text):
    """Sha256 text.

    Args: text.
    """
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def connect_db():
    """Connect db (function)."""
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.execute("PRAGMA busy_timeout=30000")
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    return conn


def ensure_hookup_tables(conn):
    """Ensure hookup tables.

    Args: conn.
    """
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS PHONE_COMPUTE_DB_NODES (
            node_id TEXT PRIMARY KEY,
            display_name TEXT NOT NULL,
            endpoint TEXT,
            role TEXT NOT NULL,
            status TEXT NOT NULL,
            notes TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS REMOTE_AGENT_HOOKUPS (
            hookup_id TEXT PRIMARY KEY,
            agent_id TEXT NOT NULL,
            endpoint TEXT,
            role TEXT NOT NULL,
            capabilities_json TEXT NOT NULL,
            resources_json TEXT NOT NULL,
            payload_sha256 TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS OFFLOAD_ACKS (
            ack_id TEXT PRIMARY KEY,
            packet_id TEXT NOT NULL,
            agent_id TEXT NOT NULL,
            received_sha256 TEXT,
            status TEXT NOT NULL,
            details_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS RESOURCE_NETWORK_NODES (
            node_id TEXT PRIMARY KEY,
            display_name TEXT NOT NULL,
            endpoint TEXT,
            node_type TEXT NOT NULL,
            capabilities_json TEXT NOT NULL,
            resources_json TEXT NOT NULL,
            score REAL NOT NULL,
            trust_level TEXT NOT NULL,
            status TEXT NOT NULL,
            last_heartbeat DATETIME DEFAULT CURRENT_TIMESTAMP,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS RESOURCE_NETWORK_TASKS (
            task_id TEXT PRIMARY KEY,
            task_type TEXT NOT NULL,
            title TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            required_capabilities_json TEXT NOT NULL,
            max_resource_class TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS RESOURCE_NETWORK_ASSIGNMENTS (
            assignment_id TEXT PRIMARY KEY,
            task_id TEXT NOT NULL,
            node_id TEXT NOT NULL,
            assignment_json TEXT NOT NULL,
            lease_seconds INTEGER NOT NULL,
            status TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            expires_at DATETIME
        );

        CREATE TABLE IF NOT EXISTS RESOURCE_NETWORK_PROOFS (
            proof_id TEXT PRIMARY KEY,
            assignment_id TEXT NOT NULL,
            node_id TEXT NOT NULL,
            proof_type TEXT NOT NULL,
            input_sha256 TEXT,
            output_sha256 TEXT,
            status TEXT NOT NULL,
            details_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS MISSED_MESSAGE_RELAY (
            relay_id TEXT PRIMARY KEY,
            source_agent TEXT NOT NULL,
            target_user TEXT NOT NULL,
            source_window TEXT,
            message TEXT NOT NULL,
            message_sha256 TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'pending',
            priority INTEGER NOT NULL DEFAULT 2,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            last_presented_at DATETIME,
            confirmed_at DATETIME,
            confirmed_by TEXT
        );

        CREATE TABLE IF NOT EXISTS LOGIC_UPLINK_RECEIPTS (
            receipt_id TEXT PRIMARY KEY,
            source_agent TEXT NOT NULL,
            protocol TEXT NOT NULL,
            content_type TEXT,
            payload_sha256 TEXT NOT NULL,
            payload_preview TEXT NOT NULL,
            block_id TEXT,
            status TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS CLOUDFLARE_TUNNEL_WATCH (
            watch_id TEXT PRIMARY KEY,
            source TEXT NOT NULL,
            current_base_url TEXT,
            current_uplink_url TEXT,
            current_ledger_url TEXT,
            health_status TEXT NOT NULL,
            health_code INTEGER,
            last_error TEXT,
            changed INTEGER NOT NULL DEFAULT 0,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        """
    )


def add_missed_message(conn, source_agent, message, priority=1):
    """Add missed message.

    Args: conn, source_agent, message, priority.
    """
    digest = sha256_text(source_agent + message)
    relay_id = f"MISSED_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{digest[:12]}"
    conn.execute(
        """
        INSERT OR IGNORE INTO MISSED_MESSAGE_RELAY (
            relay_id, source_agent, target_user, source_window, message,
            message_sha256, priority
        )
        VALUES (?, ?, 'viper', ?, ?, ?, ?)
        """,
        (relay_id, source_agent, source_agent, message, sha256_text(message), priority),
    )
    return relay_id


def current_cloudflare_base_url():
    """Current cloudflare base url (function)."""
    if not CLOUDFLARE_URL_FILE.exists():
        return None
    text = CLOUDFLARE_URL_FILE.read_text(encoding="utf-8", errors="replace").strip()
    if not text.startswith("http"):
        return None
    return text.rstrip("/")


def latest_cloudflare_destination(conn):
    """Latest cloudflare destination.

    Args: conn.
    """
    row = conn.execute(
        """
        SELECT id, base_url, uplink_url, ledger_url, status
        FROM LOGIC_BLOCKCHAIN_DESTINATIONS
        ORDER BY updated_at DESC, created_at DESC
        LIMIT 1
        """
    ).fetchone()
    return row


def probe_url(url, timeout=6):
    """Probe url.

    Args: url, timeout.
    """
    try:
        request = urllib.request.Request(url, method="GET")
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read(512).decode("utf-8", errors="replace")
            return {
                "ok": 200 <= response.status < 300,
                "status": int(response.status),
                "preview": body[:180],
                "error": None,
            }
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return {"ok": False, "status": None, "preview": "", "error": str(exc)}


def watch_cloudflare_tunnel():
    """Watch cloudflare tunnel (function)."""
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        destination = latest_cloudflare_destination(conn)
        file_base = current_cloudflare_base_url()
        changed = False
        relay_ids = []

        if destination is None and file_base:
            parsed = {
                "id": "DEST_CLOUDFLARE_SHA256_LOGIC_DB",
                "base_url": file_base,
                "uplink_url": f"{file_base}/api/uplink",
                "ledger_url": f"{file_base}/api/ledger",
                "status": "ready_not_auto_shipping",
            }
            destination = parsed
            conn.execute(
                """
                INSERT INTO LOGIC_BLOCKCHAIN_DESTINATIONS (
                    id, name, base_url, uplink_url, ledger_url, protocol, mode, status
                )
                VALUES (?, 'Cloudflare SHA-256 Logic DB', ?, ?, ?, 'acl_kqml_plaintext', 'prepared_hold', ?)
                """,
                (
                    parsed["id"],
                    parsed["base_url"],
                    parsed["uplink_url"],
                    parsed["ledger_url"],
                    parsed["status"],
                ),
            )
            changed = True
        elif destination is not None:
            parsed = {
                "id": destination[0],
                "base_url": destination[1],
                "uplink_url": destination[2],
                "ledger_url": destination[3],
                "status": destination[4],
            }
            destination = parsed

        if destination and file_base and destination["base_url"] != file_base:
            destination["base_url"] = file_base
            destination["uplink_url"] = f"{file_base}/api/uplink"
            destination["ledger_url"] = f"{file_base}/api/ledger"
            destination["status"] = "ready_not_auto_shipping"
            conn.execute(
                """
                UPDATE LOGIC_BLOCKCHAIN_DESTINATIONS
                SET base_url=?, uplink_url=?, ledger_url=?, status=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """,
                (
                    destination["base_url"],
                    destination["uplink_url"],
                    destination["ledger_url"],
                    destination["status"],
                    destination["id"],
                ),
            )
            changed = True
            relay_ids.append(
                add_missed_message(
                    conn,
                    "cloudflare_tunnel_watch",
                    f"Cloudflare tunnel URL changed to {destination['base_url']}",
                    priority=1,
                )
            )

        if not destination:
            conn.execute(
                """
                INSERT INTO CLOUDFLARE_TUNNEL_WATCH (
                    watch_id, source, current_base_url, current_uplink_url, current_ledger_url,
                    health_status, health_code, last_error, changed
                )
                VALUES (?, 'watchdog', NULL, NULL, NULL, 'missing_destination', NULL, 'No destination configured', 0)
                """,
                (f"CFTUN_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}",),
            )
            conn.commit()
            return {"status": "missing_destination", "relay_ids": relay_ids}

        health_target = f"{destination['base_url']}/health"
        probe = probe_url(health_target)
        health_status = "up" if probe["ok"] else "down"

        previous = conn.execute(
            """
            SELECT health_status, current_base_url
            FROM CLOUDFLARE_TUNNEL_WATCH
            ORDER BY created_at DESC
            LIMIT 1
            """
        ).fetchone()
        previous_status = previous[0] if previous else None
        previous_base = previous[1] if previous else None

        conn.execute(
            """
            INSERT INTO CLOUDFLARE_TUNNEL_WATCH (
                watch_id, source, current_base_url, current_uplink_url, current_ledger_url,
                health_status, health_code, last_error, changed
            )
            VALUES (?, 'watchdog', ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                f"CFTUN_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}",
                destination["base_url"],
                destination["uplink_url"],
                destination["ledger_url"],
                health_status,
                probe["status"],
                probe["error"],
                1 if changed else 0,
            ),
        )

        if health_status == "down" and previous_status != "down":
            relay_ids.append(
                add_missed_message(
                    conn,
                    "cloudflare_tunnel_watch",
                    f"Cloudflare tunnel appears down for {destination['base_url']}",
                    priority=1,
                )
            )
        if health_status == "up" and previous_status == "down":
            relay_ids.append(
                add_missed_message(
                    conn,
                    "cloudflare_tunnel_watch",
                    f"Cloudflare tunnel recovered at {destination['base_url']}",
                    priority=1,
                )
            )
        if health_status == "up" and previous_base and previous_base != destination["base_url"]:
            relay_ids.append(
                add_missed_message(
                    conn,
                    "cloudflare_tunnel_watch",
                    f"Cloudflare tunnel is back up on new link {destination['base_url']}",
                    priority=1,
                )
            )

        conn.commit()
        return {
            "status": health_status,
            "base_url": destination["base_url"],
            "uplink_url": destination["uplink_url"],
            "ledger_url": destination["ledger_url"],
            "health_code": probe["status"],
            "error": probe["error"],
            "changed": changed,
            "relay_ids": relay_ids,
        }


def register_hookup(body, default_role):
    """Register hookup.

    Args: body, default_role.
    """
    agent_id = str(body.get("agent_id") or body.get("node_id") or "unknown_agent").strip()
    endpoint = body.get("endpoint")
    role = str(body.get("role") or default_role)
    capabilities = body.get("capabilities") or {}
    resources = body.get("resources") or {}
    payload = {
        "agent_id": agent_id,
        "endpoint": endpoint,
        "role": role,
        "capabilities": capabilities,
        "resources": resources,
    }
    payload_sha = sha256_text(json.dumps(payload, sort_keys=True))
    hookup_id = f"HOOKUP_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{payload_sha[:12]}"
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        existing_node = conn.execute(
            """
            SELECT endpoint, node_type, status
            FROM RESOURCE_NETWORK_NODES
            WHERE node_id=?
            """,
            (agent_id,),
        ).fetchone()
        conn.execute(
            """
            INSERT OR REPLACE INTO REMOTE_AGENT_HOOKUPS (
                hookup_id, agent_id, endpoint, role, capabilities_json,
                resources_json, payload_sha256, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, 'connected_pending_resource_fit')
            """,
            (
                hookup_id,
                agent_id,
                endpoint,
                role,
                json.dumps(capabilities, sort_keys=True),
                json.dumps(resources, sort_keys=True),
                payload_sha,
            ),
        )
        if "phone" in role.lower() or "phone" in agent_id.lower():
            conn.execute(
                """
                INSERT OR REPLACE INTO PHONE_COMPUTE_DB_NODES (
                    node_id, display_name, endpoint, role, status, notes, updated_at
                )
                VALUES (?, ?, ?, ?, 'connected_pending_resource_fit', ?, CURRENT_TIMESTAMP)
                """,
                (
                    agent_id,
                    body.get("display_name") or agent_id,
                    endpoint,
                    role,
                    "Phone/remote agent announced through public hookup endpoint.",
                ),
            )
        node = upsert_resource_node(conn, agent_id, body.get("display_name") or agent_id, endpoint, role, capabilities, resources)
        relay_id = None
        is_new_or_changed = (
            existing_node is None
            or existing_node[0] != endpoint
            or existing_node[1] != role
            or not str(existing_node[2] or "").startswith("online")
        )
        if is_new_or_changed:
            relay_id = add_missed_message(
                conn,
                agent_id,
                f"{agent_id} connected to VIPER as {role}. Endpoint: {endpoint or 'not provided'}",
            )
        conn.commit()
    return {
        "status": "connected_pending_resource_fit" if is_new_or_changed else "heartbeat_recorded",
        "hookup_id": hookup_id,
        "agent_id": agent_id,
        "payload_sha256": payload_sha,
        "resource_node": node,
        "missed_message_id": relay_id,
        "next": [
            "GET /logic/offload-packets",
            "GET /logic/block/<block_id>",
            "POST /api/offload/ack after packet received",
        ],
    }


def receive_uplink(raw_body, headers):
    """Receive uplink.

    Args: raw_body, headers.
    """
    content_type = headers.get("Content-Type")
    protocol = headers.get("X-Viper-Protocol") or "json"
    body_text = raw_body.decode("utf-8", errors="replace")
    payload_sha = sha256_text(body_text)
    receipt_id = f"UPLINK_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{payload_sha[:12]}"
    source_agent = headers.get("X-Viper-Agent") or "viperrisc-sidecar"
    block_id = None

    if "application/json" in (content_type or "").lower():
        try:
            payload = json.loads(body_text or "{}")
            source_agent = str(payload.get("source_agent") or payload.get("sender") or source_agent)
            block_id = payload.get("block_id") or payload.get("id")
        except json.JSONDecodeError:
            protocol = "json_invalid_stored_as_text"
    else:
        marker = ":block-id "
        if marker in body_text:
            block_id = body_text.split(marker, 1)[1].split()[0].strip(")")

    with connect_db() as conn:
        ensure_hookup_tables(conn)
        conn.execute(
            """
            INSERT OR IGNORE INTO LOGIC_UPLINK_RECEIPTS (
                receipt_id, source_agent, protocol, content_type, payload_sha256,
                payload_preview, block_id, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, 'accepted')
            """,
            (receipt_id, source_agent, protocol, content_type, payload_sha, body_text[:1000], block_id),
        )
        if block_id:
            conn.execute(
                """
                UPDATE LOGIC_BLOCKCHAIN_QUEUE
                SET status='received_local_uplink', shipped_at=CURRENT_TIMESTAMP
                WHERE id=?
                """,
                (block_id,),
            )
        conn.commit()

    return {
        "status": "accepted",
        "receipt_id": receipt_id,
        "payload_sha256": payload_sha,
        "block_id": block_id,
        "protocol": protocol,
    }


def resource_score(capabilities, resources):
    """Resource score.

    Args: capabilities, resources.
    """
    score = 1.0
    text = json.dumps(capabilities, sort_keys=True).lower()
    resource_text = json.dumps(resources, sort_keys=True).lower()
    if "research" in text:
        score += 1.0
    if "code" in text:
        score += 1.0
    if "sqlite" in text or "db" in text:
        score += 1.0
    if "light" in text or "phone" in resource_text:
        score += 0.5
    for key in ("storage_mb", "ram_available_mb", "disk_free_mb"):
        try:
            score += min(float(resources.get(key, 0)) / 4096.0, 2.0)
        except (TypeError, ValueError):
            pass
    return round(score, 3)


def upsert_resource_node(conn, node_id, display_name, endpoint, node_type, capabilities, resources):
    """Upsert resource node.

    Args: conn, node_id, display_name, endpoint, node_type, capabilities, resources.
    """
    score = resource_score(capabilities, resources)
    trust_level = "observed_hash_only"
    conn.execute(
        """
        INSERT INTO RESOURCE_NETWORK_NODES (
            node_id, display_name, endpoint, node_type, capabilities_json,
            resources_json, score, trust_level, status
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'online_pending_proof')
        ON CONFLICT(node_id) DO UPDATE SET
            display_name=excluded.display_name,
            endpoint=excluded.endpoint,
            node_type=excluded.node_type,
            capabilities_json=excluded.capabilities_json,
            resources_json=excluded.resources_json,
            score=excluded.score,
            status='online_pending_proof',
            last_heartbeat=CURRENT_TIMESTAMP,
            updated_at=CURRENT_TIMESTAMP
        """,
        (
            node_id,
            display_name,
            endpoint,
            node_type,
            json.dumps(capabilities, sort_keys=True),
            json.dumps(resources, sort_keys=True),
            score,
            trust_level,
        ),
    )
    return {"node_id": node_id, "score": score, "trust_level": trust_level, "status": "online_pending_proof"}


def resource_status():
    """Resource status (function)."""
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        nodes = conn.execute(
            """
            SELECT node_id, display_name, endpoint, node_type, score, trust_level, status, last_heartbeat
            FROM RESOURCE_NETWORK_NODES
            ORDER BY score DESC, last_heartbeat DESC
            """
        ).fetchall()
        tasks = conn.execute(
            """
            SELECT task_id, task_type, title, status, created_at
            FROM RESOURCE_NETWORK_TASKS
            ORDER BY created_at DESC
            LIMIT 20
            """
        ).fetchall()
        assignments = conn.execute(
            """
            SELECT assignment_id, task_id, node_id, status, expires_at
            FROM RESOURCE_NETWORK_ASSIGNMENTS
            ORDER BY created_at DESC
            LIMIT 20
            """
        ).fetchall()
    return {
        "nodes": [
            {
                "node_id": row[0],
                "display_name": row[1],
                "endpoint": row[2],
                "node_type": row[3],
                "score": row[4],
                "trust_level": row[5],
                "status": row[6],
                "last_heartbeat": row[7],
            }
            for row in nodes
        ],
        "tasks": [
            {"task_id": row[0], "task_type": row[1], "title": row[2], "status": row[3], "created_at": row[4]}
            for row in tasks
        ],
        "assignments": [
            {"assignment_id": row[0], "task_id": row[1], "node_id": row[2], "status": row[3], "expires_at": row[4]}
            for row in assignments
        ],
    }


def create_resource_task(body):
    """Create resource task.

    Args: body.
    """
    task_type = str(body.get("task_type") or "light_compute").strip()
    title = str(body.get("title") or "Untitled resource task").strip()
    payload = body.get("payload") or {}
    required = body.get("required_capabilities") or []
    max_resource_class = str(body.get("max_resource_class") or "light").strip()
    payload_sha = sha256_text(json.dumps(body, sort_keys=True))
    task_id = f"RTASK_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{payload_sha[:12]}"
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        conn.execute(
            """
            INSERT INTO RESOURCE_NETWORK_TASKS (
                task_id, task_type, title, payload_json,
                required_capabilities_json, max_resource_class, status
            )
            VALUES (?, ?, ?, ?, ?, ?, 'open')
            """,
            (task_id, task_type, title, json.dumps(payload, sort_keys=True), json.dumps(required), max_resource_class),
        )
        conn.commit()
    return {"status": "task_created", "task_id": task_id}


def assign_resource_task(body):
    """Assign resource task.

    Args: body.
    """
    task_id = body.get("task_id")
    lease_seconds = int(body.get("lease_seconds", 3600))
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        task = conn.execute(
            """
            SELECT task_id, task_type, title, payload_json, required_capabilities_json
            FROM RESOURCE_NETWORK_TASKS
            WHERE task_id=? AND status='open'
            """,
            (task_id,),
        ).fetchone()
        if not task:
            return {"status": "no_open_task", "task_id": task_id}
        required = json.loads(task[4] or "[]")
        nodes = conn.execute(
            """
            SELECT node_id, capabilities_json, score
            FROM RESOURCE_NETWORK_NODES
            WHERE status LIKE 'online%'
            ORDER BY score DESC, last_heartbeat DESC
            """
        ).fetchall()
        chosen = None
        for node_id, caps_json, score in nodes:
            caps_text = caps_json.lower()
            if all(str(req).lower() in caps_text for req in required):
                chosen = (node_id, score)
                break
        if not chosen and nodes:
            chosen = (nodes[0][0], nodes[0][2])
        if not chosen:
            return {"status": "no_node_available", "task_id": task_id}
        assignment_payload = {
            "task_id": task_id,
            "task_type": task[1],
            "title": task[2],
            "payload": json.loads(task[3] or "{}"),
            "node_id": chosen[0],
            "lease_seconds": lease_seconds,
        }
        assignment_sha = sha256_text(json.dumps(assignment_payload, sort_keys=True))
        assignment_id = f"ASSIGN_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{assignment_sha[:12]}"
        conn.execute(
            """
            INSERT INTO RESOURCE_NETWORK_ASSIGNMENTS (
                assignment_id, task_id, node_id, assignment_json, lease_seconds,
                status, expires_at
            )
            VALUES (?, ?, ?, ?, ?, 'leased', datetime('now', '+' || ? || ' seconds'))
            """,
            (assignment_id, task_id, chosen[0], json.dumps(assignment_payload, sort_keys=True), lease_seconds, lease_seconds),
        )
        conn.execute("UPDATE RESOURCE_NETWORK_TASKS SET status='leased', updated_at=CURRENT_TIMESTAMP WHERE task_id=?", (task_id,))
        relay_id = add_missed_message(conn, chosen[0], f"{chosen[0]} received resource task {task_id}: {task[2]}")
        conn.commit()
    return {
        "status": "assigned",
        "assignment_id": assignment_id,
        "task_id": task_id,
        "node_id": chosen[0],
        "node_score": chosen[1],
        "missed_message_id": relay_id,
    }


def submit_resource_proof(body):
    """Submit resource proof.

    Args: body.
    """
    assignment_id = str(body.get("assignment_id") or "").strip()
    node_id = str(body.get("node_id") or "unknown_node").strip()
    proof_type = str(body.get("proof_type") or "execution").strip()
    input_sha = body.get("input_sha256")
    output_sha = body.get("output_sha256")
    status = str(body.get("status") or "submitted").strip()
    details = body.get("details") or {}
    proof_sha = sha256_text(json.dumps(body, sort_keys=True))
    proof_id = f"RPROOF_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{proof_sha[:12]}"
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        conn.execute(
            """
            INSERT INTO RESOURCE_NETWORK_PROOFS (
                proof_id, assignment_id, node_id, proof_type, input_sha256,
                output_sha256, status, details_json
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (proof_id, assignment_id, node_id, proof_type, input_sha, output_sha, status, json.dumps(details, sort_keys=True)),
        )
        conn.execute("UPDATE RESOURCE_NETWORK_ASSIGNMENTS SET status=? WHERE assignment_id=?", (f"proof_{status}", assignment_id))
        relay_id = add_missed_message(conn, node_id, f"{node_id} submitted proof for {assignment_id}: {status}")
        conn.commit()
    return {"status": "proof_logged", "proof_id": proof_id, "missed_message_id": relay_id}


def ack_offload(body):
    """Ack offload.

    Args: body.
    """
    packet_id = str(body.get("packet_id") or "").strip()
    agent_id = str(body.get("agent_id") or "unknown_agent").strip()
    status = str(body.get("status") or "received").strip()
    details = body.get("details") or {}
    received_sha = body.get("received_sha256")
    payload_sha = sha256_text(json.dumps(body, sort_keys=True))
    ack_id = f"ACK_{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}_{payload_sha[:12]}"
    with connect_db() as conn:
        ensure_hookup_tables(conn)
        conn.execute(
            """
            INSERT INTO OFFLOAD_ACKS (
                ack_id, packet_id, agent_id, received_sha256, status, details_json
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (ack_id, packet_id, agent_id, received_sha, status, json.dumps(details, sort_keys=True)),
        )
        conn.execute(
            """
            UPDATE PHONE_DB_OFFLOAD_PACKETS
            SET status=?
            WHERE packet_id=?
            """,
            (f"ack_{status}", packet_id),
        )
        relay_id = add_missed_message(conn, agent_id, f"{agent_id} acknowledged offload packet {packet_id}: {status}")
        conn.commit()
    return {"status": "ack_logged", "ack_id": ack_id, "packet_id": packet_id, "missed_message_id": relay_id}


def latest_blocks(limit=10):
    """Latest blocks.

    Args: limit.
    """
    with connect_db() as conn:
        rows = conn.execute(
            """
            SELECT id, payload_sha256, prev_hash, chain_hash, destination_url, status, attempts, created_at, shipped_at
            FROM LOGIC_BLOCKCHAIN_QUEUE
            ORDER BY created_at DESC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
    return [
        {
            "id": row[0],
            "payload_sha256": row[1],
            "prev_hash": row[2],
            "chain_hash": row[3],
            "destination_url": row[4],
            "status": row[5],
            "attempts": row[6],
            "created_at": row[7],
            "shipped_at": row[8],
        }
        for row in rows
    ]


def get_block(block_id):
    """Get block.

    Args: block_id.
    """
    with connect_db() as conn:
        row = conn.execute(
            """
            SELECT id, payload_sha256, prev_hash, chain_hash, destination_url,
                   status, attempts, created_at, shipped_at, payload_json
            FROM LOGIC_BLOCKCHAIN_QUEUE
            WHERE id=?
            """,
            (block_id,),
        ).fetchone()
    if not row:
        return None
    return {
        "id": row[0],
        "payload_sha256": row[1],
        "prev_hash": row[2],
        "chain_hash": row[3],
        "destination_url": row[4],
        "status": row[5],
        "attempts": row[6],
        "created_at": row[7],
        "shipped_at": row[8],
        "payload": json.loads(row[9]),
    }


def offload_packets(limit=10):
    """Offload packets.

    Args: limit.
    """
    with connect_db() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS PHONE_DB_OFFLOAD_PACKETS (
                packet_id TEXT PRIMARY KEY,
                target_node TEXT NOT NULL,
                source_table TEXT NOT NULL,
                source_id TEXT NOT NULL,
                payload_sha256 TEXT NOT NULL,
                packet_path TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        rows = conn.execute(
            """
            SELECT packet_id, target_node, source_table, source_id,
                   payload_sha256, packet_path, status, created_at
            FROM PHONE_DB_OFFLOAD_PACKETS
            ORDER BY created_at DESC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
    return [
        {
            "packet_id": row[0],
            "target_node": row[1],
            "source_table": row[2],
            "source_id": row[3],
            "payload_sha256": row[4],
            "packet_path": row[5],
            "status": row[6],
            "created_at": row[7],
        }
        for row in rows
    ]


def ship_next(destination_url=None, block_id=None):
    """Ship next.

    Args: destination_url, block_id.
    """
    with connect_db() as conn:
        if block_id:
            row = conn.execute(
                """
                SELECT id, payload_sha256, payload_json, destination_url, attempts
                FROM LOGIC_BLOCKCHAIN_QUEUE
                WHERE id=?
                """,
                (block_id,),
            ).fetchone()
        else:
            row = conn.execute(
                """
                SELECT id, payload_sha256, payload_json, destination_url, attempts
                FROM LOGIC_BLOCKCHAIN_QUEUE
                WHERE status IN ('queued', 'retry')
                  AND (destination_url IS NOT NULL OR ? IS NOT NULL)
                ORDER BY
                  CASE WHEN destination_url IS NOT NULL THEN 0 ELSE 1 END,
                  created_at ASC
                LIMIT 1
                """,
                (destination_url,),
            ).fetchone()
        if not row:
            return {"status": "empty"}

        block_id, payload_sha256, payload_json, stored_destination, attempts = row
        target = destination_url or stored_destination
        if not target:
            return {"status": "held", "id": block_id, "reason": "no_destination_url"}

        try:
            payload = payload_json.encode("utf-8")
            headers = {
                "Content-Type": "application/json",
                "X-Viper-Logic-Only": "true",
            }
            if target.rstrip("/").endswith("/api/uplink"):
                payload_obj = json.loads(payload_json)
                command = (
                    "(tell :sender viperrisc-sidecar "
                    ":content "
                    f"(logic-sha256-sync :schema {payload_obj.get('schema')} "
                    f":block-id {block_id} "
                    f":payload-sha256 {payload_sha256} "
                    f":logic-only true "
                    f":chunks {len(payload_obj.get('chunks', []))} "
                    f":candidates {len(payload_obj.get('candidates', []))} "
                    f":success-logic {len(payload_obj.get('success_logic', []))} "
                    f":liked-feedback {len(payload_obj.get('liked_feedback', []))}))"
                )
                payload = command.encode("utf-8")
                headers = {
                    "Content-Type": "text/plain; charset=utf-8",
                    "X-Viper-Logic-Only": "true",
                    "X-Viper-Protocol": "ACL-KQML",
                }

            request = urllib.request.Request(
                target,
                data=payload,
                headers=headers,
                method="POST",
            )
            with urllib.request.urlopen(request, timeout=10) as response:
                response_body = response.read(4096).decode("utf-8", errors="replace")
                response_status = response.status

            conn.execute(
                """
                UPDATE LOGIC_BLOCKCHAIN_QUEUE
                SET status='shipped', attempts=?, shipped_at=?
                WHERE id=?
                """,
                (attempts + 1, datetime.now(timezone.utc).isoformat(), block_id),
            )
            return {
                "status": "shipped",
                "id": block_id,
                "response_status": response_status,
                "response_preview": response_body[:512],
            }
        except (urllib.error.URLError, TimeoutError, OSError) as exc:
            conn.execute(
                """
                UPDATE LOGIC_BLOCKCHAIN_QUEUE
                SET status='retry', attempts=?
                WHERE id=?
                """,
                (attempts + 1, block_id),
            )
            return {"status": "retry", "id": block_id, "error": str(exc)}


class LogicBlockchainHandler(BaseHTTPRequestHandler):
    """LogicBlockchainHandler (class)."""
    server_version = "VIPERLogicBlockchainShipper/1.0"

    def do_GET(self):
        """Do GET (function)."""
        if self.path == "/health":
            tunnel = watch_cloudflare_tunnel()
            send_json(
                self,
                200,
                {
                    "status": "ok",
                    "port": LOGIC_SHIPPER_PORT,
                    "mode": "logic_only_sidecar",
                    "main_gui_untouched": True,
                    "cloudflare_tunnel": tunnel,
                },
            )
        elif self.path.startswith("/logic/blocks"):
            send_json(self, 200, {"blocks": latest_blocks()})
        elif self.path.startswith("/logic/block/"):
            block_id = self.path.split("/logic/block/", 1)[1].split("?", 1)[0]
            block = get_block(block_id)
            if block:
                send_json(self, 200, {"block": block})
            else:
                send_json(self, 404, {"error": "block_not_found", "id": block_id})
        elif self.path.startswith("/logic/offload-packets"):
            send_json(self, 200, {"packets": offload_packets()})
        elif self.path.startswith("/api/resource/status"):
            send_json(self, 200, resource_status())
        elif self.path.startswith("/api/cloudflare/status"):
            send_json(self, 200, watch_cloudflare_tunnel())
        else:
            send_json(self, 404, {"error": "not_found"})

    def do_POST(self):
        """Do POST (function)."""
        if self.path == "/logic/queue":
            body = get_body(self)
            block = queue_logic_payload(body.get("destination_url"), int(body.get("limit", 32)))
            send_json(self, 200, {"queued": block})
        elif self.path == "/logic/ship":
            body = get_body(self)
            send_json(self, 200, ship_next(body.get("destination_url"), body.get("block_id")))
        elif self.path == "/api/uplink":
            raw_body = get_raw_body(self)
            send_json(self, 202, receive_uplink(raw_body, self.headers))
        elif self.path in ("/api/phone/hookup", "/api/agent/heartbeat"):
            body = get_body(self)
            default_role = "phone_db_lend_node" if self.path == "/api/phone/hookup" else "remote_agent"
            send_json(self, 200, register_hookup(body, default_role))
        elif self.path == "/api/offload/ack":
            body = get_body(self)
            send_json(self, 200, ack_offload(body))
        elif self.path == "/api/resource/task":
            body = get_body(self)
            send_json(self, 200, create_resource_task(body))
        elif self.path == "/api/resource/assign":
            body = get_body(self)
            send_json(self, 200, assign_resource_task(body))
        elif self.path == "/api/resource/proof":
            body = get_body(self)
            send_json(self, 200, submit_resource_proof(body))
        else:
            send_json(self, 404, {"error": "not_found"})

    def log_message(self, format, *args):
        """Log message.

        Args: format.
        """
        log_path = Path(r"C:\Users\viper\VIPER_JAVA_RISC\logic_blockchain_shipper.log")
        with log_path.open("a", encoding="utf-8") as f:
            f.write(f"[{datetime.now(timezone.utc).isoformat()}] {self.address_string()} {format % args}\n")


if __name__ == "__main__":
    migrate()
    server = ThreadingHTTPServer(("127.0.0.1", LOGIC_SHIPPER_PORT), LogicBlockchainHandler)
    print(f"LOGIC_BLOCKCHAIN_SHIPPER_READY port={LOGIC_SHIPPER_PORT}")
    server.serve_forever()
