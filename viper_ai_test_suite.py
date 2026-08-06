from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import sqlite3
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(r"C:\Users\viper\VIPER_JAVA_RISC")
DB_PATH = Path(r"C:\Users\viper\gemini_bridge.db")
BRIDGE = "http://127.0.0.1:8080"
SHIPPER = "http://127.0.0.1:18081"
HOUSE = "http://127.0.0.1:11435"
JAVA_SDK = "http://127.0.0.1:18181"
PUBLIC_SHIPPER = "https://electoral-backing-coast-coordinate.trycloudflare.com"
LAB_REPORT_DIR = ROOT / "testing_lab_reports"


REQUIRED_TABLES = {
    "CHAT_MEMORY",
    "SYSTEM_TEST_LOG",
    "MISSED_MESSAGE_RELAY",
    "LOGIC_UPLINK_RECEIPTS",
    "REMOTE_AGENT_HOOKUPS",
    "RESOURCE_NETWORK_NODES",
    "RESOURCE_NETWORK_TASKS",
    "RESOURCE_NETWORK_ASSIGNMENTS",
    "RESOURCE_NETWORK_PROOFS",
    "LOGIC_BLOCKCHAIN_QUEUE",
}


def now_id() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def request_json(method: str, url: str, payload: dict[str, Any] | None = None, timeout: float = 10.0) -> tuple[int, dict[str, Any]]:
    data = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        data = json.dumps(payload, sort_keys=True).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as response:
        raw = response.read().decode("utf-8", errors="replace")
        try:
            body = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            body = {"raw": raw[:500]}
        return response.status, body


def connect_db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.execute("PRAGMA busy_timeout=30000")
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    conn.row_factory = sqlite3.Row
    return conn


def result(name: str, status: str, details: str, evidence: dict[str, Any] | None = None) -> dict[str, Any]:
    return {
        "name": name,
        "status": status,
        "details": details,
        "evidence": evidence or {},
    }


def run_http_check(name: str, method: str, url: str, payload: dict[str, Any] | None = None, expected: set[int] | None = None) -> dict[str, Any]:
    expected = expected or {200}
    start = time.perf_counter()
    try:
        code, body = request_json(method, url, payload=payload, timeout=12)
        elapsed_ms = round((time.perf_counter() - start) * 1000, 2)
        status = "pass" if code in expected else "fail"
        return result(
            name,
            status,
            f"HTTP {code} in {elapsed_ms}ms",
            {"url": url, "http_status": code, "elapsed_ms": elapsed_ms, "keys": sorted(body.keys())[:12], "body": body},
        )
    except urllib.error.HTTPError as exc:
        elapsed_ms = round((time.perf_counter() - start) * 1000, 2)
        return result(name, "fail", f"HTTP {exc.code} in {elapsed_ms}ms", {"url": url, "error": str(exc)})
    except Exception as exc:
        elapsed_ms = round((time.perf_counter() - start) * 1000, 2)
        return result(name, "fail", f"{type(exc).__name__}: {exc}", {"url": url, "elapsed_ms": elapsed_ms})


def check_schema() -> dict[str, Any]:
    try:
        with connect_db() as conn:
            tables = {
                row["name"]
                for row in conn.execute(
                    "SELECT name FROM sqlite_master WHERE type='table'"
                ).fetchall()
            }
            journal_mode = conn.execute("PRAGMA journal_mode").fetchone()[0]
        missing = sorted(REQUIRED_TABLES - tables)
        status = "pass" if not missing and journal_mode.lower() == "wal" else "fail"
        return result(
            "MemorySet.schema",
            status,
            "required recall tables present" if status == "pass" else "missing recall schema members",
            {"missing": missing, "journal_mode": journal_mode, "required": sorted(REQUIRED_TABLES)},
        )
    except Exception as exc:
        return result("MemorySet.schema", "fail", f"{type(exc).__name__}: {exc}")


def check_system_test_recall() -> dict[str, Any]:
    stamp = now_id()
    payload = {
        "test_name": f"viper_ai_suite_recall_{stamp}",
        "layer": "MemorySet",
        "status": "pass",
        "details": "VIPER AI suite recall write/read proof",
        "evidence": {"suite": "viper_ai_test_suite", "stamp": stamp},
    }
    try:
        _, write_body = request_json("POST", f"{BRIDGE}/api/system/tests", payload=payload, timeout=12)
        test_id = write_body.get("id")
        with connect_db() as conn:
            row = conn.execute(
                """
                SELECT id, test_name, status, sha256
                FROM SYSTEM_TEST_LOG
                WHERE id=?
                """,
                (test_id,),
            ).fetchone()
        passed = row is not None and row["test_name"] == payload["test_name"] and row["status"] == "pass"
        return result(
            "MemorySet.system_test_recall",
            "pass" if passed else "fail",
            f"wrote and read SYSTEM_TEST_LOG id={test_id}",
            {"id": test_id, "row": dict(row) if row else None, "write": write_body},
        )
    except Exception as exc:
        return result("MemorySet.system_test_recall", "fail", f"{type(exc).__name__}: {exc}")


def check_uplink(local_only: bool) -> list[dict[str, Any]]:
    stamp = now_id()
    payload = {"source_agent": "viper_ai_test_suite", "block_id": f"SUITE_{stamp}", "chunks": []}
    checks = [
        ("EndpointSet.local_uplink", f"{SHIPPER}/api/uplink"),
    ]
    if not local_only:
        checks.append(("EndpointSet.public_uplink", f"{PUBLIC_SHIPPER}/api/uplink"))
    results: list[dict[str, Any]] = []
    for name, url in checks:
        item = run_http_check(name, "POST", url, payload=payload, expected={202})
        if item["status"] == "pass":
            receipt = item["evidence"].get("body", {}).get("receipt_id")
            item["evidence"]["receipt_stored"] = receipt_exists(receipt)
        results.append(item)
    return results


def receipt_exists(receipt_id: str | None) -> bool:
    if not receipt_id:
        return False
    with connect_db() as conn:
        row = conn.execute(
            "SELECT 1 FROM LOGIC_UPLINK_RECEIPTS WHERE receipt_id=?",
            (receipt_id,),
        ).fetchone()
    return row is not None


def confirm_agent_relays(agent: str) -> int:
    with connect_db() as conn:
        conn.execute(
            """
            UPDATE MISSED_MESSAGE_RELAY
            SET status='confirmed',
                confirmed_at=CURRENT_TIMESTAMP,
                confirmed_by='viper_ai_test_suite',
                last_presented_at=COALESCE(last_presented_at, CURRENT_TIMESTAMP)
            WHERE source_agent=? AND status='pending'
            """,
            (agent,),
        )
        changed = conn.total_changes
        conn.commit()
    return changed


def check_heartbeat_dedupe() -> dict[str, Any]:
    stamp = now_id()
    agent = f"suite_heartbeat_{stamp}"
    payload = {
        "agent_id": agent,
        "endpoint": "local-suite",
        "role": "light_compute_agent",
        "capabilities": {"code": True, "test": True},
        "resources": {"storage_mb": 1},
    }
    try:
        responses = [
            request_json("POST", f"{SHIPPER}/api/agent/heartbeat", payload=payload, timeout=12)[1]
            for _ in range(3)
        ]
        with connect_db() as conn:
            relay_count = conn.execute(
                "SELECT COUNT(*) FROM MISSED_MESSAGE_RELAY WHERE source_agent=?",
                (agent,),
            ).fetchone()[0]
            node = conn.execute(
                "SELECT node_id, status, last_heartbeat FROM RESOURCE_NETWORK_NODES WHERE node_id=?",
                (agent,),
            ).fetchone()
        passed = relay_count == 1 and node is not None and responses[1].get("missed_message_id") is None and responses[2].get("missed_message_id") is None
        confirmed = confirm_agent_relays(agent)
        return result(
            "AgentSet.heartbeat_dedupe",
            "pass" if passed else "fail",
            "first heartbeat creates one notice; repeats update liveness only",
            {"agent": agent, "relay_count": relay_count, "suite_relays_confirmed": confirmed, "responses": responses, "node": dict(node) if node else None},
        )
    except Exception as exc:
        return result("AgentSet.heartbeat_dedupe", "fail", f"{type(exc).__name__}: {exc}")


def check_resource_lifecycle() -> dict[str, Any]:
    stamp = now_id()
    agent = f"suite_resource_node_{stamp}"
    try:
        heartbeat = {
            "agent_id": agent,
            "display_name": agent,
            "endpoint": "local-suite",
            "role": "light_compute_agent",
            "capabilities": {"code": True, "test": True, "genetic_coder": True},
            "resources": {"storage_mb": 1, "ram_available_mb": 1},
        }
        request_json("POST", f"{SHIPPER}/api/agent/heartbeat", payload=heartbeat, timeout=12)
        _, task = request_json(
            "POST",
            f"{SHIPPER}/api/resource/task",
            payload={
                "task_type": "genetic_coder_smoke",
                "title": f"VIPER suite resource lifecycle {stamp}",
                "payload": {"expected": "proof_only"},
                "required_capabilities": ["genetic_coder"],
                "max_resource_class": "light",
            },
            timeout=12,
        )
        _, assignment = request_json(
            "POST",
            f"{SHIPPER}/api/resource/assign",
            payload={"task_id": task.get("task_id"), "lease_seconds": 60},
            timeout=12,
        )
        proof_payload = {
            "assignment_id": assignment.get("assignment_id"),
            "node_id": assignment.get("node_id"),
            "proof_type": "execution",
            "input_sha256": sha256_text(task.get("task_id") or ""),
            "output_sha256": sha256_text(json.dumps(assignment, sort_keys=True)),
            "status": "pass",
            "details": {"suite": "viper_ai_test_suite", "stamp": stamp},
        }
        _, proof = request_json("POST", f"{SHIPPER}/api/resource/proof", payload=proof_payload, timeout=12)
        with connect_db() as conn:
            assignment_row = conn.execute(
                "SELECT assignment_id, node_id, status FROM RESOURCE_NETWORK_ASSIGNMENTS WHERE assignment_id=?",
                (assignment.get("assignment_id"),),
            ).fetchone()
            proof_row = conn.execute(
                "SELECT proof_id, assignment_id, node_id, status FROM RESOURCE_NETWORK_PROOFS WHERE proof_id=?",
                (proof.get("proof_id"),),
            ).fetchone()
        passed = (
            task.get("status") == "task_created"
            and assignment.get("status") == "assigned"
            and proof.get("status") == "proof_logged"
            and assignment_row is not None
            and str(assignment_row["status"]) == "proof_pass"
            and proof_row is not None
        )
        confirmed = confirm_agent_relays(agent)
        return result(
            "AgentSet.resource_lifecycle",
            "pass" if passed else "fail",
            "node heartbeat -> task -> assign -> proof persisted",
            {
                "agent": agent,
                "task": task,
                "assignment": assignment,
                "proof": proof,
                "suite_relays_confirmed": confirmed,
                "assignment_row": dict(assignment_row) if assignment_row else None,
                "proof_row": dict(proof_row) if proof_row else None,
            },
        )
    except Exception as exc:
        return result("AgentSet.resource_lifecycle", "fail", f"{type(exc).__name__}: {exc}")


def check_genetic_coder_smoke() -> dict[str, Any]:
    workspace = ROOT / "karoo_smoke_tests" / "tiny_program_002"
    candidates_dir = workspace / "candidates"
    try:
        candidates = sorted(
            [path for path in candidates_dir.glob("*") if path.is_dir()],
            key=lambda path: path.stat().st_mtime,
            reverse=True,
        )
        if not candidates:
            return result("ReasoningSet.genetic_coder_smoke", "fail", "no candidate directories found", {"workspace": str(workspace)})
        candidate = candidates[0]
        required = ["app.py", "test_app.py", "README.md"]
        missing = [name for name in required if not (candidate / name).exists()]
        if missing:
            return result(
                "ReasoningSet.genetic_coder_smoke",
                "fail",
                "candidate missing required files",
                {"candidate": str(candidate), "missing": missing},
            )
        compile_run = subprocess.run(
            ["py", "-3", "-m", "py_compile", "app.py", "test_app.py"],
            cwd=candidate,
            text=True,
            capture_output=True,
            timeout=20,
        )
        runtime = subprocess.run(
            ["py", "-3", "test_app.py"],
            cwd=candidate,
            text=True,
            capture_output=True,
            timeout=20,
        )
        passed = compile_run.returncode == 0 and runtime.returncode == 0
        return result(
            "ReasoningSet.genetic_coder_smoke",
            "pass" if passed else "fail",
            "latest candidate compiles and tests" if passed else "latest candidate failed compile/runtime",
            {
                "candidate": str(candidate),
                "compile_returncode": compile_run.returncode,
                "compile_stderr": compile_run.stderr[-1000:],
                "runtime_returncode": runtime.returncode,
                "runtime_stdout": runtime.stdout[-1000:],
                "runtime_stderr": runtime.stderr[-1000:],
            },
        )
    except Exception as exc:
        return result("ReasoningSet.genetic_coder_smoke", "fail", f"{type(exc).__name__}: {exc}")


def check_karoo_status() -> dict[str, Any]:
    try:
        candidates = sorted((ROOT / "topology_candidates").glob("EXP_*"), key=lambda path: path.stat().st_mtime, reverse=True)
        if not candidates:
            return result("ReasoningSet.karoo_comparator", "fail", "no topology candidates found")
        report_path = candidates[0] / "report.json"
        if not report_path.exists():
            return result("ReasoningSet.karoo_comparator", "fail", "latest candidate has no report", {"candidate": str(candidates[0])})
        report = json.loads(report_path.read_text(encoding="utf-8", errors="replace"))
        comparison_count = int(report.get("comparison_count") or 0)
        status = "pass" if comparison_count >= 3 else "degraded"
        details = "Karoo comparator has ranking evidence" if status == "pass" else "Karoo is baseline-only; comparison_count < 3"
        return result(
            "ReasoningSet.karoo_comparator",
            status,
            details,
            {"report": str(report_path), "mode": report.get("mode"), "comparison_count": comparison_count},
        )
    except Exception as exc:
        return result("ReasoningSet.karoo_comparator", "fail", f"{type(exc).__name__}: {exc}")


def check_relay_backlog() -> dict[str, Any]:
    try:
        with connect_db() as conn:
            pending = conn.execute(
                "SELECT COUNT(*) FROM MISSED_MESSAGE_RELAY WHERE status='pending'"
            ).fetchone()[0]
            unpresented = conn.execute(
                "SELECT COUNT(*) FROM MISSED_MESSAGE_RELAY WHERE status='pending' AND last_presented_at IS NULL"
            ).fetchone()[0]
        status = "pass" if pending < 100 else "degraded"
        return result(
            "MemorySet.relay_backlog",
            status,
            "relay backlog under control" if status == "pass" else "old relay backlog still needs cleanup policy",
            {"pending": pending, "unpresented": unpresented},
        )
    except Exception as exc:
        return result("MemorySet.relay_backlog", "fail", f"{type(exc).__name__}: {exc}")


def truth_report(results: list[dict[str, Any]]) -> dict[str, list[str]]:
    report = {"working": [], "degraded": [], "not_working": [], "placeholder": []}
    for item in results:
        status = item["status"]
        name = item["name"]
        if status == "pass":
            report["working"].append(name)
        elif status == "degraded":
            report["degraded"].append(name)
        else:
            report["not_working"].append(name)
    if "ReasoningSet.karoo_comparator" in report["degraded"]:
        report["placeholder"].append("Karoo optimizer ranking is not real until comparison_count >= 3.")
    return report


def summarize(results: list[dict[str, Any]]) -> dict[str, Any]:
    counts: dict[str, int] = {}
    for item in results:
        counts[item["status"]] = counts.get(item["status"], 0) + 1
    overall = "pass"
    if counts.get("fail"):
        overall = "fail"
    elif counts.get("degraded"):
        overall = "degraded"
    return {"overall": overall, "counts": counts}


def print_table(results: list[dict[str, Any]]) -> None:
    print("VIPER AI TEST SUITE")
    print("-------------------")
    for item in results:
        print(f"{item['status'].upper():8} {item['name']}: {item['details']}")


def run_predict_latency(name: str, message: str, expected_route: str, max_elapsed_ms: float) -> dict[str, Any]:
    start = time.perf_counter()
    try:
        code, body = request_json("POST", f"{BRIDGE}/api/loibi/predict", payload={"message": message}, timeout=30)
        elapsed_ms = round((time.perf_counter() - start) * 1000, 2)
        actual_route = ((body.get("lens") or {}).get("route")) or ""
        ok = code == 200 and actual_route == expected_route and elapsed_ms <= max_elapsed_ms
        status = "pass" if ok else "degraded"
        return result(
            name,
            status,
            f"route={actual_route or 'unknown'} in {elapsed_ms}ms",
            {
                "http_status": code,
                "elapsed_ms": elapsed_ms,
                "expected_route": expected_route,
                "actual_route": actual_route,
                "response_preview": str(body.get("response", ""))[:400],
            },
        )
    except Exception as exc:
        return result(name, "fail", f"{type(exc).__name__}: {exc}")


def check_behavior_pack_guardrail() -> dict[str, Any]:
    try:
        lens_path = ROOT / "tools" / "data_retrieval_lens_agent.py"
        spec = importlib.util.spec_from_file_location("viper_data_retrieval_lens_agent_suite", lens_path)
        if spec is None or spec.loader is None:
            raise RuntimeError(f"Unable to load {lens_path}")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)

        lens = module.craft_lens("plan getting the programming cube to work and analyze whole system")
        behavior_pack = lens.get("behavior_pack") or []
        nominal_context = lens.get("nominal_context") or {}
        ok = len(behavior_pack) <= 5 and isinstance(nominal_context, dict)
        return result(
            "BehaviorSet.pack_guardrail",
            "pass" if ok else "fail",
            f"behavior_pack={len(behavior_pack)} nominal_keys={sorted(nominal_context.keys())}",
            {
                "behavior_pack_size": len(behavior_pack),
                "behavior_pack": behavior_pack,
                "nominal_context_keys": sorted(nominal_context.keys()),
            },
        )
    except Exception as exc:
        return result("BehaviorSet.pack_guardrail", "fail", f"{type(exc).__name__}: {exc}")


def check_distillation_memory() -> dict[str, Any]:
    try:
        with connect_db() as conn:
            success_count = conn.execute("SELECT COUNT(*) FROM SUCCESSFUL_CODE_ADVANCES").fetchone()[0]
            queue_count = conn.execute("SELECT COUNT(*) FROM KAROO_DISTILLATION_QUEUE").fetchone()[0]
            fact_count = conn.execute("SELECT COUNT(*) FROM USER_NOMINAL_FACTS").fetchone()[0]
        status = "pass" if success_count > 0 and queue_count > 0 else "degraded"
        return result(
            "BehaviorSet.distillation_memory",
            status,
            f"successes={success_count} queue={queue_count} nominal_facts={fact_count}",
            {
                "successful_code_advances": success_count,
                "karoo_distillation_queue": queue_count,
                "user_nominal_facts": fact_count,
            },
        )
    except Exception as exc:
        return result("BehaviorSet.distillation_memory", "fail", f"{type(exc).__name__}: {exc}")


def check_benchmark_snapshot() -> dict[str, Any]:
    try:
        code, body = request_json("POST", f"{JAVA_SDK}/api/benchmark-snapshot", payload={"reason": "testing_lab_probe"}, timeout=15)
        ok = code == 200 and str(body.get("kind", "")).startswith("benchmark_snapshot")
        return result(
            "BenchmarkSet.snapshot",
            "pass" if ok else "fail",
            f"HTTP {code} benchmark snapshot",
            {"http_status": code, "body": body},
        )
    except Exception as exc:
        return result("BenchmarkSet.snapshot", "fail", f"{type(exc).__name__}: {exc}")


def check_java_sdk_epoch_queue() -> dict[str, Any]:
    try:
        code, body = request_json("GET", f"{JAVA_SDK}/api/ascii-epochs?limit=3", timeout=10)
        queue = body.get("queue") or []
        status = "pass" if code == 200 else "fail"
        return result(
            "EpochSet.ascii_queue_read",
            status,
            f"HTTP {code} queue_entries={len(queue)}",
            {"http_status": code, "queue_size": len(queue), "body": body},
        )
    except Exception as exc:
        return result("EpochSet.ascii_queue_read", "fail", f"{type(exc).__name__}: {exc}")


def check_java_sdk_epoch_append() -> dict[str, Any]:
    payload = {
        "subsystem": "db_retrieval",
        "quickVar": "retrieval_weight",
        "judgeSlot": "local_benchmark",
        "note": f"testing_lab_epoch_{now_id()}",
    }
    try:
        code, body = request_json("POST", f"{JAVA_SDK}/api/ascii-epochs", payload=payload, timeout=15)
        ok = code == 200 and body.get("kind") == "ascii_epoch_proposal"
        return result(
            "EpochSet.ascii_queue_append",
            "pass" if ok else "fail",
            f"HTTP {code} ascii epoch append",
            {"http_status": code, "body": body},
        )
    except Exception as exc:
        return result("EpochSet.ascii_queue_append", "fail", f"{type(exc).__name__}: {exc}")


def check_epoch_upgrade_proof() -> dict[str, Any]:
    payload = {"goal": "testing_lab_probe", "rule": "proposal_only_no_auto_apply"}
    try:
        code, body = request_json("POST", f"{JAVA_SDK}/api/epoch-upgrade-proof", payload=payload, timeout=20)
        ok = code == 200 and body.get("kind") == "epoch_upgrade_proof"
        return result(
            "EpochSet.upgrade_proof",
            "pass" if ok else "fail",
            f"HTTP {code} epoch proof",
            {"http_status": code, "body": body},
        )
    except Exception as exc:
        return result("EpochSet.upgrade_proof", "fail", f"{type(exc).__name__}: {exc}")


def check_recent_fault_pressure() -> dict[str, Any]:
    try:
        log_path = ROOT / "system_log.txt"
        lines = log_path.read_text(encoding="utf-8", errors="replace").splitlines()[-300:]
        lock_hits = sum(1 for line in lines if "database is locked" in line.lower())
        timeout_hits = sum(1 for line in lines if "house_timeout_or_fault" in line.lower())
        thin_hits = sum(1 for line in lines if "thin_response_detected" in line.lower())
        if lock_hits or timeout_hits:
            status = "degraded"
            details = f"recent lock_hits={lock_hits} timeout_hits={timeout_hits} thin_hits={thin_hits}"
        else:
            status = "pass"
            details = f"no recent lock or timeout hits; thin_hits={thin_hits}"
        return result(
            "ChaosSet.fault_pressure",
            status,
            details,
            {"lock_hits": lock_hits, "timeout_hits": timeout_hits, "thin_hits": thin_hits, "log_path": str(log_path)},
        )
    except Exception as exc:
        return result("ChaosSet.fault_pressure", "fail", f"{type(exc).__name__}: {exc}")


def testing_lab_sections(args: argparse.Namespace) -> dict[str, list[dict[str, Any]]]:
    smoke = [
        run_http_check("SmokeSet.gui_bridge_datapoints", "GET", f"{BRIDGE}/api/datapoints"),
        run_http_check("SmokeSet.system_tests_api", "GET", f"{BRIDGE}/api/system/tests?limit=1"),
        run_http_check("SmokeSet.logic_shipper", "GET", f"{SHIPPER}/health"),
        run_http_check("SmokeSet.house_health", "GET", f"{HOUSE}/health"),
        run_http_check("SmokeSet.java_sdk", "GET", f"{JAVA_SDK}/health"),
        run_predict_latency("SmokeSet.chat_predict", "hello", "chat", 2000),
    ]
    system = [
        check_schema(),
        check_system_test_recall(),
        *check_uplink(local_only=args.local_only),
        check_heartbeat_dedupe(),
        check_resource_lifecycle(),
        check_genetic_coder_smoke(),
        check_karoo_status(),
        run_predict_latency("SystemSet.planning_predict", "plan getting the programming cube to work and analyze whole system", "planning", 2500),
        run_predict_latency("SystemSet.build_predict", "make a db vector point edit mode for blobs", "build", 2000),
    ]
    benchmark = [
        run_http_check("BenchmarkSet.bridge_read", "GET", f"{BRIDGE}/api/benchmarks?limit=5"),
        run_http_check("BenchmarkSet.shipper_resource_status", "GET", f"{SHIPPER}/api/resource/status"),
        check_benchmark_snapshot(),
        run_predict_latency("BenchmarkSet.planning_latency", "plan getting the programming cube to work and analyze whole system", "planning", 2500),
        run_predict_latency("BenchmarkSet.build_latency", "make a db vector point edit mode for blobs", "build", 2000),
    ]
    behavioral = [
        check_behavior_pack_guardrail(),
        check_distillation_memory(),
        check_relay_backlog(),
    ]
    epoch = [
        run_http_check("EpochSet.java_sdk_health", "GET", f"{JAVA_SDK}/health"),
        check_java_sdk_epoch_queue(),
        check_java_sdk_epoch_append(),
        check_epoch_upgrade_proof(),
    ]
    chaos = [
        check_recent_fault_pressure(),
        check_relay_backlog(),
        run_predict_latency("ChaosSet.planning_guardrail", "plan getting the programming cube to work and analyze whole system", "planning", 2500),
    ]
    return {
        "smoke": smoke,
        "system": system,
        "benchmark": benchmark,
        "behavioral": behavioral,
        "epoch": epoch,
        "chaos": chaos,
    }


def flatten_sections(selected: dict[str, list[dict[str, Any]]]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for section_results in selected.values():
        rows.extend(section_results)
    return rows


def write_lab_report(section: str, selected: dict[str, list[dict[str, Any]]]) -> Path:
    LAB_REPORT_DIR.mkdir(parents=True, exist_ok=True)
    results = flatten_sections(selected)
    payload = {
        "kind": "testing_lab_report",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "section": section,
        "summary": summarize(results),
        "truth": truth_report(results),
        "sections": selected,
    }
    path = LAB_REPORT_DIR / f"{now_id()}_{section}_testing_lab_report.json"
    path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    return path


def record_lab_summary(section: str, report_path: Path, summary: dict[str, Any]) -> None:
    payload = {
        "test_name": f"testing_lab_{section}_{now_id()}",
        "layer": "TestingLab",
        "status": summary["overall"],
        "details": f"Testing lab {section} summary recorded",
        "evidence": {
            "report_path": str(report_path),
            "summary": summary,
        },
    }
    try:
        request_json("POST", f"{BRIDGE}/api/system/tests", payload=payload, timeout=12)
    except Exception:
        pass


def run_status(args: argparse.Namespace) -> int:
    results: list[dict[str, Any]] = [
        run_http_check("ServiceSet.gui_bridge", "GET", f"{BRIDGE}/api/datapoints"),
        run_http_check("ServiceSet.system_tests_api", "GET", f"{BRIDGE}/api/system/tests?limit=1"),
        run_http_check("ServiceSet.logic_shipper", "GET", f"{SHIPPER}/health"),
        run_http_check("ServiceSet.house_health", "GET", f"{HOUSE}/health"),
        check_schema(),
        check_system_test_recall(),
        *check_uplink(local_only=args.local_only),
        check_heartbeat_dedupe(),
        check_resource_lifecycle(),
        check_genetic_coder_smoke(),
        check_karoo_status(),
        check_relay_backlog(),
    ]
    summary = summarize(results)
    if args.json:
        print(json.dumps({"summary": summary, "truth": truth_report(results), "results": results}, indent=2))
    else:
        print_table(results)
        print("-------------------")
        print(f"OVERALL  {summary['overall'].upper()} {summary['counts']}")
        truth = truth_report(results)
        print(f"WORKING  {len(truth['working'])} checks")
        print(f"DEGRADED {len(truth['degraded'])} checks")
        print(f"FAILED   {len(truth['not_working'])} checks")
    return 0 if summary["overall"] == "pass" else 1


def run_lab(args: argparse.Namespace) -> int:
    sections = testing_lab_sections(args)
    if args.section == "full":
        selected = sections
    else:
        selected = {args.section: sections[args.section]}
    results = flatten_sections(selected)
    summary = summarize(results)
    truth = truth_report(results)
    report_path = write_lab_report(args.section, selected)
    record_lab_summary(args.section, report_path, summary)
    if args.json:
        print(json.dumps({
            "summary": summary,
            "truth": truth,
            "report_path": str(report_path),
            "sections": selected,
        }, indent=2))
    else:
        print_table(results)
        print("-------------------")
        print(f"LAB      {args.section.upper()} {summary['overall'].upper()} {summary['counts']}")
        print(f"REPORT   {report_path}")
        print(f"WORKING  {len(truth['working'])} checks")
        print(f"DEGRADED {len(truth['degraded'])} checks")
        print(f"FAILED   {len(truth['not_working'])} checks")
    return 0 if summary["overall"] == "pass" else 1


def run_node_card(args: argparse.Namespace) -> int:
    base = args.base_url.rstrip("/")
    agent_id = args.agent_id
    card = {
        "agent_id": agent_id,
        "display_name": args.display_name or agent_id,
        "endpoint": args.endpoint,
        "role": args.role,
        "capabilities": {
            "code": True,
            "test": True,
            "genetic_coder": True,
            "sqlite": True,
            "proof": True,
        },
        "resources": {
            "storage_mb": args.storage_mb,
            "ram_available_mb": args.ram_available_mb,
        },
    }
    powershell = (
        "$body = @'\n"
        + json.dumps(card, indent=2)
        + "\n'@; "
        + f"Invoke-RestMethod -Method POST -Uri '{base}/api/agent/heartbeat' -ContentType 'application/json' -Body $body"
    )
    payload = {
        "card": card,
        "heartbeat_url": f"{base}/api/agent/heartbeat",
        "resource_status_url": f"{base}/api/resource/status",
        "proof_url": f"{base}/api/resource/proof",
        "powershell": powershell,
    }
    print(json.dumps(payload, indent=2))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="VIPER real-AI red/green test suite")
    sub = parser.add_subparsers(dest="command", required=True)
    status = sub.add_parser("status", help="Run live ServiceSet/MemorySet/EndpointSet proof checks.")
    status.add_argument("--json", action="store_true", help="Emit machine-readable JSON.")
    status.add_argument("--local-only", action="store_true", help="Skip public Cloudflare uplink check.")
    lab = sub.add_parser("lab", help="Run the sectioned testing lab: smoke, system, benchmark, behavioral, epoch, chaos, or full.")
    lab.add_argument("--section", choices=["smoke", "system", "benchmark", "behavioral", "epoch", "chaos", "full"], default="full")
    lab.add_argument("--json", action="store_true", help="Emit machine-readable JSON.")
    lab.add_argument("--local-only", action="store_true", help="Skip public Cloudflare uplink check.")
    node_card = sub.add_parser("node-card", help="Print a ready-to-run node heartbeat card for another computer.")
    node_card.add_argument("--agent-id", required=True)
    node_card.add_argument("--display-name", default="")
    node_card.add_argument("--endpoint", default="")
    node_card.add_argument("--role", default="light_compute_agent")
    node_card.add_argument("--base-url", default=PUBLIC_SHIPPER)
    node_card.add_argument("--storage-mb", type=int, default=512)
    node_card.add_argument("--ram-available-mb", type=int, default=512)
    args = parser.parse_args()
    if args.command == "status":
        return run_status(args)
    if args.command == "lab":
        return run_lab(args)
    if args.command == "node-card":
        return run_node_card(args)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
