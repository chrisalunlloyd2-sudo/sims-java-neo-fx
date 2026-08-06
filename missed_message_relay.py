from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
from datetime import datetime, timezone
from pathlib import Path


DB_PATH = Path(r"C:\Users\viper\gemini_bridge.db")


def now_id() -> str:
    """Now id (function)."""
    return datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")


def now_iso() -> str:
    """Now iso (function)."""
    return datetime.now(timezone.utc).isoformat()


def sha256_text(text: str) -> str:
    """Sha256 text.

    Args: text.
    """
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def connect() -> sqlite3.Connection:
    """Connect (function)."""
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.execute("PRAGMA busy_timeout=30000")
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    return conn


def migrate() -> None:
    """Migrate (function)."""
    with connect() as conn:
        conn.executescript(
            """
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

            CREATE TABLE IF NOT EXISTS MISSED_MESSAGE_PRESENTATIONS (
                presentation_id TEXT PRIMARY KEY,
                relay_id TEXT NOT NULL,
                presented_to_window TEXT NOT NULL,
                presentation_text TEXT NOT NULL,
                presentation_sha256 TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );

            CREATE INDEX IF NOT EXISTS idx_missed_relay_status_priority
            ON MISSED_MESSAGE_RELAY(status, priority, created_at);

            CREATE INDEX IF NOT EXISTS idx_missed_presentations_relay
            ON MISSED_MESSAGE_PRESENTATIONS(relay_id, created_at);
            """
        )


def add_message(source_agent: str, message: str, source_window: str | None = None, target_user: str = "viper", priority: int = 2) -> dict:
    """Add message.

    Args: source_agent, message, source_window, target_user, priority.
    """
    migrate()
    digest = sha256_text(source_agent + target_user + message)
    relay_id = f"MISSED_{now_id()}_{digest[:12]}"
    with connect() as conn:
        conn.execute(
            """
            INSERT OR IGNORE INTO MISSED_MESSAGE_RELAY (
                relay_id, source_agent, target_user, source_window, message,
                message_sha256, priority
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (relay_id, source_agent, target_user, source_window, message, sha256_text(message), priority),
        )
    return {"relay_id": relay_id, "status": "pending", "message_sha256": sha256_text(message)}


def pending(window: str, limit: int = 5) -> dict:
    """Pending.

    Args: window, limit.
    """
    migrate()
    with connect() as conn:
        rows = conn.execute(
            """
            SELECT relay_id, source_agent, message, priority, created_at
            FROM MISSED_MESSAGE_RELAY
            WHERE status='pending'
            ORDER BY priority ASC, created_at ASC
            LIMIT ?
            """,
            (limit,),
        ).fetchall()
        notices = []
        for relay_id, source_agent, message, priority, created_at in rows:
            text = f"{source_agent} finished some work: {message}"
            presentation_hash = sha256_text(relay_id + window + text + now_iso())
            presentation_id = f"PRESENT_{now_id()}_{presentation_hash[:10]}"
            conn.execute(
                """
                INSERT INTO MISSED_MESSAGE_PRESENTATIONS (
                    presentation_id, relay_id, presented_to_window,
                    presentation_text, presentation_sha256
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                (presentation_id, relay_id, window, text, presentation_hash),
            )
            conn.execute(
                """
                UPDATE MISSED_MESSAGE_RELAY
                SET last_presented_at=CURRENT_TIMESTAMP
                WHERE relay_id=?
                """,
                (relay_id,),
            )
            notices.append(
                {
                    "relay_id": relay_id,
                    "source_agent": source_agent,
                    "priority": priority,
                    "created_at": created_at,
                    "notice": text,
                }
            )
        conn.commit()
    return {"window": window, "pending_count": len(notices), "notices": notices}


def confirm(relay_id: str, confirmed_by: str) -> dict:
    """Confirm.

    Args: relay_id, confirmed_by.
    """
    migrate()
    with connect() as conn:
        conn.execute(
            """
            UPDATE MISSED_MESSAGE_RELAY
            SET status='confirmed', confirmed_at=CURRENT_TIMESTAMP, confirmed_by=?
            WHERE relay_id=?
            """,
            (confirmed_by, relay_id),
        )
        changed = conn.total_changes
    return {"relay_id": relay_id, "confirmed_by": confirmed_by, "changed": changed}


def status() -> dict:
    """Status (function)."""
    migrate()
    with connect() as conn:
        rows = conn.execute(
            """
            SELECT status, COUNT(*)
            FROM MISSED_MESSAGE_RELAY
            GROUP BY status
            ORDER BY status
            """
        ).fetchall()
    return {"counts": dict(rows)}


def cleanup_generated_noise(dry_run: bool = True) -> dict:
    """Cleanup generated noise.

    Args: dry_run.
    """
    migrate()
    patterns = [
        ("unknown_agent", "unknown_agent connected to VIPER as %Endpoint: not provided%"),
        ("suite_%", "suite_%"),
    ]
    with connect() as conn:
        targets = []
        for source_like, message_like in patterns:
            rows = conn.execute(
                """
                SELECT relay_id, source_agent, message, created_at
                FROM MISSED_MESSAGE_RELAY
                WHERE status='pending'
                  AND source_agent LIKE ?
                  AND message LIKE ?
                ORDER BY created_at ASC
                """,
                (source_like, message_like),
            ).fetchall()
            targets.extend(rows)
        unique = {row[0]: row for row in targets}
        if not dry_run and unique:
            conn.executemany(
                """
                UPDATE MISSED_MESSAGE_RELAY
                SET status='confirmed',
                    confirmed_at=CURRENT_TIMESTAMP,
                    confirmed_by='viper_ai_cleanup_policy',
                    last_presented_at=COALESCE(last_presented_at, CURRENT_TIMESTAMP)
                WHERE relay_id=?
                """,
                [(relay_id,) for relay_id in unique],
            )
            conn.commit()
    return {
        "dry_run": dry_run,
        "matched": len(unique),
        "policy": "confirm generated unknown_agent no-endpoint heartbeat noise and suite probe notices only",
        "sample": [
            {
                "relay_id": row[0],
                "source_agent": row[1],
                "message": row[2][:160],
                "created_at": row[3],
            }
            for row in list(unique.values())[:10]
        ],
    }


def main() -> int:
    """Main (function)."""
    parser = argparse.ArgumentParser(description="VIPER missed-message relay for cross-window agent notices.")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("migrate")
    add = sub.add_parser("add")
    add.add_argument("source_agent")
    add.add_argument("message")
    add.add_argument("--source-window", default=None)
    add.add_argument("--target-user", default="viper")
    add.add_argument("--priority", type=int, default=2)
    pend = sub.add_parser("pending")
    pend.add_argument("--window", default="unknown_window")
    pend.add_argument("--limit", type=int, default=5)
    conf = sub.add_parser("confirm")
    conf.add_argument("relay_id")
    conf.add_argument("--by", default="viper")
    sub.add_parser("status")
    cleanup = sub.add_parser("cleanup-generated-noise")
    cleanup.add_argument("--apply", action="store_true", help="Confirm generated noise rows. Without this, only reports matches.")
    args = parser.parse_args()

    if args.command == "migrate":
        migrate()
        print("MISSED_MESSAGE_RELAY_READY")
    elif args.command == "add":
        print(json.dumps(add_message(args.source_agent, args.message, args.source_window, args.target_user, args.priority), indent=2))
    elif args.command == "pending":
        print(json.dumps(pending(args.window, args.limit), indent=2))
    elif args.command == "confirm":
        print(json.dumps(confirm(args.relay_id, args.by), indent=2))
    elif args.command == "status":
        print(json.dumps(status(), indent=2))
    elif args.command == "cleanup-generated-noise":
        print(json.dumps(cleanup_generated_noise(dry_run=not args.apply), indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
