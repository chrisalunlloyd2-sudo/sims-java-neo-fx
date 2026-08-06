import argparse
import json
import os
import re
import shutil
import subprocess
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path


ROOT = Path(r"C:\Users\viper\VIPER_JAVA_RISC")
DEFAULT_CONFIG = ROOT / "ops" / "command_center_maintenance" / "maintenance_policy.json"


def now_utc() -> datetime:
    """Now utc (function)."""
    return datetime.now(timezone.utc)


def iso_now() -> str:
    """Iso now (function)."""
    return now_utc().strftime("%Y-%m-%dT%H:%M:%SZ")


def load_config(path: Path) -> dict:
    """Load config.

    Args: path.
    """
    return json.loads(path.read_text(encoding="utf-8"))


def ensure_dir(path: Path) -> Path:
    """Ensure dir.

    Args: path.
    """
    path.mkdir(parents=True, exist_ok=True)
    return path


def write_json(path: Path, payload: dict) -> None:
    """Write json.

    Args: path, payload.
    """
    path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def write_text(path: Path, text: str) -> None:
    """Write text.

    Args: path, text.
    """
    path.write_text(text, encoding="utf-8")


def slug(text: str) -> str:
    """Slug.

    Args: text.
    """
    return re.sub(r"[^A-Za-z0-9._-]+", "_", text).strip("_") or "item"


def parse_project_entries(config: dict) -> list[dict]:
    """Parse project entries.

    Args: config.
    """
    desktop_root = Path(config["desktop_root"])
    projects = []
    for entry in config["projects"]:
        project = dict(entry)
        project.setdefault("folder_path", str(desktop_root / project["name"]))
        project["folder_path"] = str(Path(project["folder_path"]))
        if project.get("source_path"):
            project["source_path"] = str(Path(project["source_path"]))
        projects.append(project)
    return projects


def ensure_project_placeholders(config: dict) -> dict:
    """Ensure project placeholders.

    Args: config.
    """
    created = []
    updated = []
    for project in parse_project_entries(config):
        folder = ensure_dir(Path(project["folder_path"]))
        readme = folder / "README_PLACEHOLDER.md"
        content = (
            f"# {project['name']}\n\n"
            f"- status: {project.get('status', 'placeholder_pending_hookup')}\n"
            f"- canonical source: {project.get('source_path', 'pending hookup')}\n"
            f"- node owner: {project.get('node_owner', 'pending')}\n"
            f"- created by command-center maintenance bootstrap\n"
        )
        if not readme.exists():
            readme.write_text(content, encoding="utf-8")
            created.append(str(folder))
        else:
            updated.append(str(folder))
    return {"created": created, "existing": updated}


def path_recent(path: Path) -> datetime:
    """Path recent.

    Args: path.
    """
    try:
        return datetime.fromtimestamp(path.stat().st_mtime, tz=timezone.utc)
    except FileNotFoundError:
        return datetime.fromtimestamp(0, tz=timezone.utc)


def cleanup_temp_files(config: dict) -> dict:
    """Cleanup temp files.

    Args: config.
    """
    report = {"deleted": [], "errors": [], "error_count": 0}
    cutoff = now_utc() - timedelta(days=int(config["temp_cleanup"]["older_than_days"]))
    for root_text in config["temp_cleanup"]["roots"]:
        root = Path(os.path.expandvars(root_text))
        if not root.exists():
            continue
        for dirpath, _, filenames in os.walk(root, topdown=True, onerror=lambda _: None):
            for filename in filenames:
                path = Path(dirpath) / filename
                try:
                    if path_recent(path) < cutoff:
                        path.unlink(missing_ok=True)
                        report["deleted"].append(str(path))
                except OSError as exc:
                    report["error_count"] += 1
                    if len(report["errors"]) < 100:
                        report["errors"].append({"path": str(path), "error": str(exc)})
    return report


def rotate_logs(config: dict) -> dict:
    """Rotate logs.

    Args: config.
    """
    report = {"rotated": [], "missing": []}
    log_cfg = config["log_rotation"]
    version_root = ensure_dir(Path(log_cfg["version_root"]))
    stamp = now_utc().strftime("%Y%m%dT%H%M%SZ")
    version_dir = ensure_dir(version_root / stamp)
    max_bytes = int(log_cfg["truncate_if_over_mb"] * 1024 * 1024)

    for item in log_cfg["files"]:
        path = Path(item)
        if not path.exists():
            report["missing"].append(str(path))
            continue
        size = path.stat().st_size
        if size < max_bytes:
            continue
        target = version_dir / f"{path.name}.{stamp}.bak"
        shutil.copy2(path, target)
        path.write_text("", encoding="utf-8")
        report["rotated"].append({"file": str(path), "backup": str(target), "size": size})
    return report


def desktop_archive_candidates(config: dict) -> dict:
    """Desktop archive candidates.

    Args: config.
    """
    desktop_root = Path(config["desktop_root"])
    archive_root = ensure_dir(Path(config["archive_root"]))
    keep = set(config["keep_desktop_items"])
    cutoff = now_utc() - timedelta(days=int(config["desktop_window_days"]))
    report = {"candidates": [], "archive_root": str(archive_root), "apply_enabled": bool(config["archive_apply"])}

    for item in desktop_root.iterdir():
        if item.name in keep:
            continue
        if item.name in {entry["name"] for entry in config["projects"]}:
            continue
        if path_recent(item) >= cutoff:
            continue
        report["candidates"].append(
            {"path": str(item), "last_modified": path_recent(item).strftime("%Y-%m-%dT%H:%M:%SZ")}
        )

    if config["archive_apply"]:
        moved = []
        for candidate in report["candidates"]:
            source = Path(candidate["path"])
            target = archive_root / source.name
            if target.exists():
                target = archive_root / f"{source.name}_{now_utc().strftime('%Y%m%dT%H%M%SZ')}"
            shutil.move(str(source), str(target))
            moved.append({"from": str(source), "to": str(target)})
        report["moved"] = moved
    return report


def scrub_sensitive_candidates(config: dict) -> dict:
    """Scrub sensitive candidates.

    Args: config.
    """
    scrub_cfg = config["scrub_policy"]
    roots = [Path(p) for p in scrub_cfg["roots"]]
    name_patterns = [re.compile(x, re.IGNORECASE) for x in scrub_cfg["filename_patterns"]]
    text_patterns = [re.compile(x, re.IGNORECASE) for x in scrub_cfg["content_patterns"]]
    max_bytes = int(scrub_cfg["content_scan_max_bytes"])
    max_files = int(scrub_cfg["max_files"])
    max_matches = int(scrub_cfg["max_matches"])
    allowed_suffixes = {suffix.lower() for suffix in scrub_cfg["file_extensions"]}
    excludes = {Path(p).resolve() for p in scrub_cfg["exclude_paths"]}
    matches = []
    files_scanned = 0

    def is_excluded(path: Path) -> bool:
        try:
            resolved = path.resolve()
        except OSError:
            return True
        return any(str(resolved).startswith(str(ex)) for ex in excludes)

    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if files_scanned >= max_files or len(matches) >= max_matches:
                break
            if not path.is_file() or is_excluded(path):
                continue
            if path.suffix.lower() not in allowed_suffixes:
                continue
            files_scanned += 1
            hit_reason = None
            if any(p.search(path.name) for p in name_patterns):
                hit_reason = "filename_pattern"
            elif path.stat().st_size <= max_bytes:
                try:
                    body = path.read_text(encoding="utf-8", errors="ignore")
                except OSError:
                    body = ""
                if any(p.search(body) for p in text_patterns):
                    hit_reason = "content_pattern"
            if hit_reason:
                matches.append({"path": str(path), "reason": hit_reason})
    return {
        "files_scanned": files_scanned,
        "match_count": len(matches),
        "apply_enabled": bool(scrub_cfg["apply_quarantine"]),
        "matches": matches[:500],
    }


def env_optimize_report(config: dict) -> dict:
    """Env optimize report.

    Args: config.
    """
    cmd = (
        "Get-CimInstance Win32_Process | "
        "Where-Object { $_.Name -in @('python.exe','py.exe','java.exe','javaw.exe','node.exe','cloudflared.exe') } | "
        "Select-Object ProcessId,Name,CommandLine | ConvertTo-Json -Depth 4"
    )
    output = subprocess.run(
        ["powershell", "-NoProfile", "-Command", cmd],
        check=True,
        capture_output=True,
        text=True,
    ).stdout.strip()
    rows = json.loads(output) if output else []
    if isinstance(rows, dict):
        rows = [rows]
    protected_terms = [x.lower() for x in config["env_optimize"]["protected_terms"]]
    self_terms = ["command_center_maintenance.py"]
    candidates = []
    protected = []
    for row in rows:
        line = (row.get("CommandLine") or row.get("Name") or "").lower()
        item = {
            "pid": row.get("ProcessId"),
            "name": row.get("Name"),
            "command_line": row.get("CommandLine"),
        }
        if any(term in line for term in protected_terms):
            protected.append(item)
        elif any(term in line for term in self_terms):
            protected.append(item)
        else:
            candidates.append(item)
    return {
        "protected": protected,
        "stale_candidates": candidates,
        "stop_enabled": False,
        "note": "Report-only by default. Promote exact kill rules with a later global command.",
    }


def scan_todos(path: Path, limit: int = 40) -> list[str]:
    """Scan todos.

    Args: path, limit.
    """
    hits = []
    if not path.exists():
        return hits
    for file in path.rglob("*"):
        if not file.is_file():
            continue
        if file.suffix.lower() not in {".md", ".py", ".ps1", ".java", ".js", ".ts", ".json", ".txt"}:
            continue
        try:
            for idx, line in enumerate(file.read_text(encoding="utf-8", errors="ignore").splitlines(), start=1):
                if "TODO" in line.upper():
                    hits.append(f"{file}:L{idx} {line.strip()[:180]}")
                    if len(hits) >= limit:
                        return hits
        except OSError:
            continue
    return hits


def ascii_tree(path: Path, max_depth: int = 3, max_entries: int = 160) -> str:
    """Ascii tree.

    Args: path, max_depth, max_entries.
    """
    if not path.exists():
        return f"{path.name}\n  [missing]"
    lines = [path.name]
    count = 0

    def walk(folder: Path, prefix: str, depth: int) -> None:
        nonlocal count
        if depth > max_depth or count >= max_entries:
            return
        try:
            children = sorted(folder.iterdir(), key=lambda p: (not p.is_dir(), p.name.lower()))
        except OSError:
            return
        for index, child in enumerate(children):
            if count >= max_entries:
                return
            connector = "`-- " if index == len(children) - 1 else "|-- "
            lines.append(f"{prefix}{connector}{child.name}")
            count += 1
            if child.is_dir():
                extension = "    " if index == len(children) - 1 else "|   "
                walk(child, prefix + extension, depth + 1)

    walk(path, "", 0)
    return "\n".join(lines)


def project_doc_bundle(config: dict) -> dict:
    """Project doc bundle.

    Args: config.
    """
    sync_root = ensure_dir(Path(config["sync_root"]))
    local_root = ensure_dir(ROOT / "ops" / "command_center_maintenance" / "generated" / now_utc().strftime("%Y-%m-%d"))
    sync_out = ensure_dir(sync_root / "weekly_project_docs" / now_utc().strftime("%Y-%m-%d"))
    bundles = []

    for project in parse_project_entries(config):
        project_name = project["name"]
        source = Path(project.get("source_path") or project["folder_path"])
        folder = ensure_dir(local_root / slug(project_name))
        todo_hits = scan_todos(source)
        tree_text = ascii_tree(source)
        meta = {
            "project": project_name,
            "source_path": str(source),
            "folder_path": project["folder_path"],
            "exists": source.exists(),
            "last_modified": path_recent(source).strftime("%Y-%m-%dT%H:%M:%SZ") if source.exists() else None,
            "todo_count": len(todo_hits),
            "node_owner": project.get("node_owner"),
        }
        write_json(folder / "PROJECT_META.json", meta)
        write_text(folder / "ASCII_TREE.txt", tree_text + "\n")
        write_text(
            folder / "README.md",
            "\n".join(
                [
                    f"# {project_name}",
                    "",
                    f"- source path: {source}",
                    f"- desktop folder: {project['folder_path']}",
                    f"- node owner: {project.get('node_owner', 'pending')}",
                    f"- exists now: {source.exists()}",
                    f"- todo count: {len(todo_hits)}",
                    "",
                    "## Current TODO",
                    *(f"- {item}" for item in todo_hits[:25]),
                    "",
                    "## Summary",
                    "- Generated by Friday project documentation sweep.",
                    "- Update source_path mapping when a phone or other laptop project is hooked in.",
                ]
            )
            + "\n",
        )
        write_text(
            folder / "BLUEPRINT.md",
            "\n".join(
                [
                    f"# {project_name} Blueprint",
                    "",
                    "## Axiomatic Set",
                    "- Input surfaces",
                    "- Build surfaces",
                    "- Proof surfaces",
                    "- Ship surfaces",
                    "",
                    "## Topological Tree",
                    "See ASCII_TREE.txt",
                    "",
                    "## Build Tree",
                    "- bootstrap",
                    "- implement",
                    "- verify",
                    "- ship",
                    "",
                    "## Updated TODO",
                    *(f"- {item}" for item in todo_hits[:25]),
                ]
            )
            + "\n",
        )
        target = ensure_dir(sync_out / slug(project_name))
        for file in folder.iterdir():
            shutil.copy2(file, target / file.name)
        bundles.append({"project": project_name, "output": str(target), "source": str(source), "todo_count": len(todo_hits)})
    return {"local_root": str(local_root), "sync_root": str(sync_out), "bundles": bundles}


def emit_acl_kqml_commands(config: dict, mode: str) -> dict:
    """Emit acl kqml commands.

    Args: config, mode.
    """
    outbox = ensure_dir(Path(config["sync_root"]) / "acl_kqml_outbox")
    stamp = now_utc().strftime("%Y%m%dT%H%M%SZ")
    generated = []
    for node in config["nodes"]:
        receiver = node["agent_id"]
        actions = node["actions"].get(mode, [])
        if not actions:
            continue
        content = {
            "mode": mode,
            "desktop_root": config["desktop_root"],
            "sync_root": config["sync_root"],
            "archive_root": config["archive_root"],
            "projects": [project["name"] for project in config["projects"]],
            "actions": actions,
        }
        content_json = json.dumps(content).replace('"', '\\"')
        message = (
            "(achieve\n"
            '  :sender "COMMAND_CENTER"\n'
            f'  :receiver "{receiver}"\n'
            '  :language "json"\n'
            '  :ontology "viper.maintenance.acl_kqml"\n'
            f'  :conversation-id "maintenance-{stamp}-{slug(receiver)}"\n'
            f'  :reply-with "maintenance-{slug(mode)}"\n'
            f'  :content "{content_json}"\n'
            ")\n"
        )
        path = outbox / f"{stamp}_{slug(receiver)}_{slug(mode)}.kqml"
        path.write_text(message, encoding="utf-8")
        generated.append(str(path))
    return {"outbox": str(outbox), "generated": generated}


def write_report(config: dict, mode: str, payload: dict) -> Path:
    """Write report.

    Args: config, mode, payload.
    """
    root = ensure_dir(ROOT / "ops" / "command_center_maintenance" / "reports")
    path = root / f"{now_utc().strftime('%Y%m%dT%H%M%SZ')}_{slug(mode)}_report.json"
    write_json(path, payload)
    return path


def daily_run(config: dict) -> dict:
    """Daily run.

    Args: config.
    """
    payload = {
        "mode": "daily",
        "timestamp": iso_now(),
        "projects": ensure_project_placeholders(config),
        "temp_cleanup": cleanup_temp_files(config),
        "log_rotation": rotate_logs(config),
        "desktop_archive": desktop_archive_candidates(config),
        "scrub_report": scrub_sensitive_candidates(config),
        "env_optimize": env_optimize_report(config),
        "acl_kqml": emit_acl_kqml_commands(config, "daily"),
    }
    payload["report_path"] = str(write_report(config, "daily", payload))
    return payload


def weekly_docs_run(config: dict) -> dict:
    """Weekly docs run.

    Args: config.
    """
    payload = {
        "mode": "weekly_docs",
        "timestamp": iso_now(),
        "projects": ensure_project_placeholders(config),
        "docs": project_doc_bundle(config),
        "acl_kqml": emit_acl_kqml_commands(config, "weekly_docs"),
    }
    payload["report_path"] = str(write_report(config, "weekly_docs", payload))
    return payload


def main() -> int:
    """Main (function)."""
    parser = argparse.ArgumentParser(description="VIPER command-center maintenance orchestrator")
    parser.add_argument("--config", default=str(DEFAULT_CONFIG))
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("ensure-projects")
    sub.add_parser("daily")
    sub.add_parser("weekly-docs")
    args = parser.parse_args()

    config = load_config(Path(args.config))
    if args.command == "ensure-projects":
        print(json.dumps(ensure_project_placeholders(config), indent=2))
        return 0
    if args.command == "daily":
        print(json.dumps(daily_run(config), indent=2))
        return 0
    if args.command == "weekly-docs":
        print(json.dumps(weekly_docs_run(config), indent=2))
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
