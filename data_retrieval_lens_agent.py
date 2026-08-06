import argparse
import hashlib
import json
import os
import random
import re
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path


HOME = Path(r"C:\Users\viper")
ROOT = HOME / "VIPER_JAVA_RISC"
DB_PATH = HOME / "gemini_bridge.db"
FABRIC_SOURCE = HOME / ".old" / "AIEngine" / "external_sources" / "fabric"
LENS_DB_BUSY_TIMEOUT_MS = int(os.environ.get("VIPER_LENS_DB_BUSY_TIMEOUT_MS", "3000"))
TOOLS_DIR = Path(__file__).resolve().parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

try:
    from tiny_model_runtime import (
        axiomatic_retrieval_match,
        model_status as tiny_model_status,
        qwen_choose_lens,
        qwen_rolling_triplet_card,
    )

    HAS_TINY_RUNTIME = True
except Exception as tiny_import_error:
    HAS_TINY_RUNTIME = False
    TINY_IMPORT_ERROR = str(tiny_import_error)

STOPWORDS = {
    "the", "and", "for", "with", "that", "this", "from", "into", "your",
    "you", "can", "could", "would", "should", "have", "has", "are", "was",
    "were", "but", "not", "all", "any", "our", "out", "then", "than",
}

BUILD_TERMS = {
    "build", "make", "create", "write", "patch", "fix", "implement",
    "code", "program", "app", "application", "automate", "ship", "deploy",
    "download", "install", "spin", "wire", "hook", "sync", "backup", "stage",
    "fork", "crawl", "benchmark", "test", "edit", "update", "delete", "remove",
}

PLAN_TERMS = {
    "plan", "architecture", "design", "breakdown", "strategy", "protocol",
    "epoch", "logic", "topological", "network", "agent", "agents", "swarm",
    "karoo", "loihi", "acl", "kqml", "fabric", "architechture",
    "reasoning", "rationale", "chain", "thought", "novel",
    "compare", "winner", "merge", "genetic", "upgrade", "standards",
    "checkpoint", "epoch", "nas", "github", "loihi", "lava", "analyze",
}

CHAT_TERMS = {
    "hello", "hi", "thanks", "thank", "lol", "nice", "what", "why",
    "explain", "think", "feel", "question",
}

SOURCE_TRUST_WEIGHTS = {
    "USER_TOPOLOGY_PROFILE": 9,
    "CHAT_MEMORY": 8,
    "CODE_BLOCKCHAIN_DB_SUCCESS": 10,
    "LOGIC_BLOCKCHAIN_QUEUE_SHIPPED": 10,
    "BLOCKCHAIN_LEDGER_SUCCESS": 9,
    "KAROO_CANDIDATES": 8,
    "TOPO_APPROVAL_REPORTS": 8,
    "TOPO_CANDIDATES": 8,
    "TOPO_CHUNKS": 7,
    "GLOBAL_TODO_QUEUE": 7,
    "GLOBAL_ACL_MESSAGES": 7,
    "TRIPLET_MANIFOLD": 6,
    "RAG_MANIFOLD": 6,
    "GAME_DATA": 6,
    "WEBCRAWL_RESEARCH_REQUESTS": 5,
    "INDUSTRY_RESEARCH_NOTES": 9,
}

GENERIC_SCAN_EXCLUDE = {
    "AI_CHOOSER_ACTIVE_LENSES",
    "AI_CHOOSER_REVIEWS",
    "DATA_RETRIEVAL_EVENTS",
    "FABRIC_LENSES",
    "FABRIC_TEMPLATE_SNAPSHOTS",
    "PREDICTIVE_PREFETCH_LOG",
    "CHAT_TOPOLOGICAL_LOCATION",
    "CONVERSATION_BINOMIAL_SUMMARY",
    "SECURITY_SENTINEL_SEEN",
    "sqlite_sequence",
    "USER_TOPOLOGY_PROFILE",
    "CHAT_MEMORY",
    "CODE_BLOCKCHAIN_DB",
    "LOGIC_BLOCKCHAIN_QUEUE",
    "BLOCKCHAIN_LEDGER",
    "TRIPLET_MANIFOLD",
    "RAG_MANIFOLD",
    "TOPO_CHUNKS",
    "TOPO_APPROVAL_REPORTS",
    "GLOBAL_TODO_QUEUE",
    "GLOBAL_ACL_MESSAGES",
    "GAME_DATA",
    "WEBCRAWL_RESEARCH_REQUESTS",
    "SYSTEM_TEST_LOG",
    "BENCHMARK_EVENTS",
}

_MIGRATION_DONE = False


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def sha256_text(text):
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def unique_prefix(prefix, ask_sha):
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    return f"{prefix}_{stamp}_{ask_sha[:10]}_{random.randrange(16**4):04x}"


def connect_db():
    conn = sqlite3.connect(DB_PATH, timeout=max(1, LENS_DB_BUSY_TIMEOUT_MS // 1000))
    conn.execute(f"PRAGMA busy_timeout={LENS_DB_BUSY_TIMEOUT_MS}")
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    conn.row_factory = sqlite3.Row
    return conn


def migrate(conn):
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS DATA_RETRIEVAL_EVENTS (
            event_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            ask_preview TEXT NOT NULL,
            route TEXT NOT NULL,
            token_limit INTEGER NOT NULL,
            lens_id TEXT NOT NULL,
            result_count INTEGER NOT NULL,
            decision_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS FABRIC_LENSES (
            lens_id TEXT PRIMARY KEY,
            route TEXT NOT NULL,
            ask_sha256 TEXT NOT NULL,
            token_limit INTEGER NOT NULL,
            lens_text TEXT NOT NULL,
            lens_sha256 TEXT NOT NULL,
            sources_json TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'active',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS KAROO_EPOCH_REQUESTS (
            epoch_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            route TEXT NOT NULL,
            loop_count INTEGER NOT NULL,
            actor_critic TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'proposal_only',
            contract_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS FABRIC_TEMPLATE_SNAPSHOTS (
            template_id TEXT PRIMARY KEY,
            route TEXT NOT NULL,
            ask_sha256 TEXT NOT NULL,
            template_text TEXT NOT NULL,
            template_sha256 TEXT NOT NULL,
            hooks_json TEXT NOT NULL,
            noise_policy_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS WEBCRAWL_RESEARCH_REQUESTS (
            request_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            route TEXT NOT NULL,
            query_json TEXT NOT NULL,
            noise_policy_json TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'queued_proposal_only',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS AI_CHOOSER_REVIEWS (
            review_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            lens_id TEXT NOT NULL,
            template_id TEXT,
            route TEXT NOT NULL,
            draft_lens_sha256 TEXT NOT NULL,
            reviewed_lens_sha256 TEXT,
            review_status TEXT NOT NULL DEFAULT 'queued',
            review_json TEXT NOT NULL DEFAULT '{}',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            reviewed_at DATETIME
        );

        CREATE TABLE IF NOT EXISTS AXIOMATIC_RETRIEVAL_MATCHES (
            match_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            route TEXT NOT NULL,
            fabric_layer TEXT NOT NULL,
            match_text TEXT NOT NULL,
            status TEXT NOT NULL,
            candidate_count INTEGER NOT NULL,
            model_meta_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS TINY_MODEL_EVENTS (
            event_id TEXT PRIMARY KEY,
            ask_sha256 TEXT,
            model_role TEXT NOT NULL,
            status TEXT NOT NULL,
            details_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS ROLLING_TRIPLET_RUNS (
            triplet_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            route TEXT NOT NULL,
            fabric_layer TEXT NOT NULL,
            chooser_lens_text TEXT NOT NULL,
            retrieval_match_text TEXT NOT NULL,
            rolling_card_text TEXT NOT NULL,
            status TEXT NOT NULL,
            meta_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS USER_WORD_STATS (
            term TEXT PRIMARY KEY,
            count INTEGER NOT NULL DEFAULT 0,
            route_hits_json TEXT NOT NULL DEFAULT '{}',
            last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS USER_TOPOLOGICAL_WANTS (
            want_key TEXT PRIMARY KEY,
            count INTEGER NOT NULL DEFAULT 0,
            last_ask_sha256 TEXT NOT NULL,
            last_route TEXT NOT NULL,
            last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS BENCHMARK_EVENTS (
            benchmark_id TEXT PRIMARY KEY,
            component TEXT NOT NULL,
            operation TEXT NOT NULL,
            route TEXT,
            duration_ms INTEGER NOT NULL,
            status TEXT NOT NULL,
            details_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS CHAT_STATE_SNAPSHOTS (
            snapshot_id TEXT PRIMARY KEY,
            ask_sha256 TEXT NOT NULL,
            route TEXT NOT NULL,
            state_key TEXT NOT NULL,
            summary_15 TEXT NOT NULL,
            tokens_json TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS CHAT_STATE_TRANSITIONS (
            transition_id TEXT PRIMARY KEY,
            from_state TEXT NOT NULL,
            to_state TEXT NOT NULL,
            route TEXT NOT NULL,
            count INTEGER NOT NULL DEFAULT 1,
            last_ask_sha256 TEXT NOT NULL,
            sample_from_summary TEXT NOT NULL,
            sample_to_summary TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS USER_NOMINAL_FACTS (
            fact_key TEXT PRIMARY KEY,
            fact_value TEXT NOT NULL,
            fact_type TEXT NOT NULL,
            confidence REAL NOT NULL DEFAULT 0.5,
            source_sha256 TEXT NOT NULL,
            source_excerpt TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS SUCCESSFUL_CODE_ADVANCES (
            advance_id TEXT PRIMARY KEY,
            source_kind TEXT NOT NULL,
            source_id TEXT NOT NULL,
            route TEXT NOT NULL,
            summary_text TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            payload_sha256 TEXT NOT NULL UNIQUE,
            confidence REAL NOT NULL DEFAULT 0.0,
            status TEXT NOT NULL DEFAULT 'verified',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE TABLE IF NOT EXISTS KAROO_DISTILLATION_QUEUE (
            queue_id TEXT PRIMARY KEY,
            source_kind TEXT NOT NULL,
            source_id TEXT NOT NULL,
            route TEXT NOT NULL,
            summary_text TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            payload_sha256 TEXT NOT NULL UNIQUE,
            priority INTEGER NOT NULL DEFAULT 2,
            status TEXT NOT NULL DEFAULT 'queued',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );

        CREATE INDEX IF NOT EXISTS idx_retrieval_ask ON DATA_RETRIEVAL_EVENTS(ask_sha256);
        CREATE INDEX IF NOT EXISTS idx_fabric_lenses_route ON FABRIC_LENSES(route);
        CREATE INDEX IF NOT EXISTS idx_karoo_epoch_status ON KAROO_EPOCH_REQUESTS(status);
        CREATE INDEX IF NOT EXISTS idx_fabric_template_route ON FABRIC_TEMPLATE_SNAPSHOTS(route);
        CREATE INDEX IF NOT EXISTS idx_webcrawl_research_status ON WEBCRAWL_RESEARCH_REQUESTS(status);
        CREATE INDEX IF NOT EXISTS idx_ai_chooser_reviews_status ON AI_CHOOSER_REVIEWS(review_status);
        CREATE INDEX IF NOT EXISTS idx_axiomatic_matches_route ON AXIOMATIC_RETRIEVAL_MATCHES(route, created_at);
        CREATE INDEX IF NOT EXISTS idx_tiny_model_events_role ON TINY_MODEL_EVENTS(model_role, created_at);
        CREATE INDEX IF NOT EXISTS idx_rolling_triplet_route ON ROLLING_TRIPLET_RUNS(route, created_at);
        CREATE INDEX IF NOT EXISTS idx_benchmark_events_component ON BENCHMARK_EVENTS(component, operation);
        CREATE INDEX IF NOT EXISTS idx_chat_state_snapshots_route ON CHAT_STATE_SNAPSHOTS(route, created_at);
        CREATE INDEX IF NOT EXISTS idx_chat_state_transitions_from_route ON CHAT_STATE_TRANSITIONS(from_state, route, count);
        CREATE INDEX IF NOT EXISTS idx_user_nominal_facts_type ON USER_NOMINAL_FACTS(fact_type, updated_at);
        CREATE INDEX IF NOT EXISTS idx_successful_code_advances_route ON SUCCESSFUL_CODE_ADVANCES(route, created_at);
        CREATE INDEX IF NOT EXISTS idx_karoo_distillation_queue_status ON KAROO_DISTILLATION_QUEUE(status, priority, created_at);
        """
    )


def ensure_migrated(conn):
    global _MIGRATION_DONE
    if _MIGRATION_DONE:
        return
    migrate(conn)
    conn.commit()
    _MIGRATION_DONE = True


def tokenize(text, limit=32):
    words = re.findall(r"[A-Za-z0-9_#.+-]{3,}", text.lower())
    scored = []
    seen = set()
    for word in words:
        if word in STOPWORDS or word in seen:
            continue
        seen.add(word)
        score = len(word)
        if word in BUILD_TERMS or word in PLAN_TERMS:
            score += 12
        scored.append((score, word))
    scored.sort(reverse=True)
    return [word for _, word in scored[:limit]]


def is_short_chat_fast_path(text):
    words = re.findall(r"\S+", str(text or ""))
    if len(words) > 6:
        return False
    return bool(re.fullmatch(
        r"\s*(?:hi|hello|hey|yo|sup|hiya|howdy|ok|okay|nice|cool|great|thanks|thank you|sounds good|go ahead|please continue|alright|yes please)[!.?\s]*",
        str(text or ""),
        re.IGNORECASE,
    ))


def is_infra_build_fast_path(text, decision):
    if decision.get("route") != "build":
        return False
    lower = str(text or "").lower()
    if re.search(r"\b[a-z]:\\|\.py\b|\.java\b|/|\\", lower):
        return False
    infra_terms = ("karoo", "database", "db", "speed", "distill", "queue", "topology", "success")
    return any(term in lower for term in infra_terms)


def classify_ask(text):
    lower = text.lower()
    if is_short_chat_fast_path(text):
        return {
            "route": "chat",
            "token_limit": 256,
            "scores": {
                "build": 0,
                "planning": 0,
                "chat": 3,
                "word_count": len(re.findall(r"\S+", text)),
            },
            "tokens": tokenize(text),
        }
    tokens = set(tokenize(text, 80))
    build_score = sum(1 for term in BUILD_TERMS if term in tokens or term in lower)
    plan_score = sum(1 for term in PLAN_TERMS if term in tokens or term in lower)
    chat_score = sum(1 for term in CHAT_TERMS if term in tokens or term in lower)
    user_performative_phrases = (
        "spin up", "wire", "hook", "sync", "ship", "checkpoint",
        "upgrade epoch", "ping", "open notes", "backup", "crawl",
        "benchmark", "stage", "fork", "deploy", "download", "install",
    )
    if any(phrase in lower for phrase in user_performative_phrases):
        build_score += 2
    has_directive = bool(re.search(
        r"\b(can you|please|make sure|make|add|implement|fix|build|create|edit|update|delete|remove|wire|hook|spin up|test|download|install)\b",
        lower,
    ))

    if build_score >= 2 or (has_directive and build_score >= 1):
        route = "build"
    elif plan_score >= 2 or (has_directive and plan_score >= 1):
        route = "planning"
    else:
        route = "chat"

    word_count = len(re.findall(r"\S+", text))
    if route == "chat":
        token_limit = 1024 if word_count < 90 else 1536
    elif route == "planning":
        token_limit = 3072 if word_count < 220 else 4096
    else:
        token_limit = 4096 if word_count < 260 else 6144

    return {
        "route": route,
        "token_limit": token_limit,
        "scores": {
            "build": build_score,
            "planning": plan_score,
            "chat": chat_score,
            "word_count": word_count,
        },
        "tokens": tokenize(text),
    }


def table_exists(conn, table):
    row = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?", (table,)
    ).fetchone()
    return row is not None


def score_text(text, tokens):
    lower = text.lower()
    score = 0
    for token in tokens:
        if token in lower:
            score += 1 + min(5, lower.count(token))
    return score


def compact_words(text, limit=15, char_limit=220):
    words = re.findall(r"\S+", str(text or ""))
    compact = " ".join(words[:limit])
    if len(compact) > char_limit:
        return compact[: max(0, char_limit - 3)] + "..."
    return compact


def source_summary_text(data, source_name):
    data = data or {}
    preferred_keys = (
        "claim",
        "summary",
        "summary_text",
        "details",
        "description",
        "message",
        "output",
        "title",
        "status",
    )
    lead = []
    for key in preferred_keys:
        value = data.get(key)
        if value:
            lead.append(str(value))
    if not lead:
        for key, value in list(data.items())[:3]:
            if value:
                lead.append(f"{key}={value}")
    summary = " | ".join(lead)
    summary = re.sub(r"\s+", " ", summary).strip(" |")
    if not summary:
        return f"{source_name} row with limited readable fields"
    return summary


def deterministic_match_summary(candidates):
    if not candidates:
        return "Source: none. Match: no strong database row found for this ask."
    item = candidates[0] if isinstance(candidates[0], dict) else {}
    source = item.get("source", "unknown")
    card = item.get("card", {}) if isinstance(item, dict) else {}
    summary = (
        card.get("summary")
        or card.get("card_15")
        or source_summary_text(item.get("data", {}), source)
    )
    applicability = card.get("applicability", "")
    parts = [
        f"Source: {source}.",
        f"Match: {compact_words(summary, 24, 240)}.",
    ]
    if applicability:
        parts.append(f"Use: {compact_words(applicability, 14, 140)}.")
    return " ".join(parts)


def infer_purpose(ask, decision):
    lower = ask.lower()
    if decision["route"] == "build":
        action = "perform_task"
        purpose = "turn the user request into one safe code/change proposal with compile/test proof"
    elif decision["route"] == "planning":
        action = "plan_or_architect"
        purpose = "create a useful operating architecture, protocol, or next-step plan"
    else:
        action = "commence_chat"
        purpose = "answer conversationally with enough context and no unnecessary tool/agent loop"

    if any(term in lower for term in ("hurt", "finger", "cut", "pain")):
        purpose += "; keep typing burden low and avoid asking unnecessary questions"
    if any(term in lower for term in ("retrieval", "retreival", "db", "database", "data")):
        purpose += "; improve DB-backed evidence retrieval before response"
    if any(term in lower for term in ("web", "research", "industry", "standard", "standards")):
        purpose += "; include reduced web/research snippets when local DB lacks enough evidence"

    return {
        "purpose": purpose,
        "action": action,
        "route": decision["route"],
        "ask_card_15": compact_words(ask, 15),
        "success_criteria": [
            "answer uses the retrieved evidence packet",
            "irrelevant DB rows are excluded or marked low confidence",
            "web snippets are reduced to claims + source + applicability + risk",
            "task directions are explicit enough for the model/agent to act",
        ],
    }


def tiny_prompt_engineer_card(purpose, decision, web_plan):
    if decision["route"] == "build":
        directive = (
            "Use the evidence cards to perform the smallest safe build step. Prefer successful ledger code. "
            "If evidence is insufficient, request or queue reduced web snippets. Return action, proof, and next test."
        )
    elif decision["route"] == "planning":
        directive = (
            "Compare the candidate systems, choose a winner for VIPER, explain why, then give the merged architecture "
            "and next test. Use evidence cards; do not drift into generic summaries."
        )
    else:
        directive = (
            "Answer naturally but ground the reply in the purpose and evidence cards. Do not dump raw retrieval. "
            "If the user asks to compare, choose a winner."
        )
    if web_plan.get("status") == "queued_if_needed":
        directive += " Web snippets are claim/source/hash/applicability/risk only."
    return compact_words(directive, 50, 420)


def query_variants(ask, tokens, route):
    base = " ".join(tokens[:10])
    variants = [
        ask,
        base,
        " ".join(tokens[:6]),
    ]
    if route == "build":
        variants.extend([
            "successful code compile test " + base,
            "karoo candidate shipped logic " + base,
        ])
    elif route == "planning":
        variants.extend([
            "architecture protocol topology " + base,
            "sop agent workflow evidence " + base,
        ])
    else:
        variants.extend([
            "user preference recent chat " + base,
            "direct answer context " + base,
        ])
    clean = []
    seen = set()
    for variant in variants:
        normalized = re.sub(r"\s+", " ", variant.strip())
        if normalized and normalized not in seen:
            clean.append(normalized)
            seen.add(normalized)
    return clean[:6]


def behavior_state_key(decision):
    lead = decision["tokens"][:4]
    if not lead:
        return f"{decision['route']}::empty"
    return f"{decision['route']}::{'_'.join(lead)}"


def record_markov_state(conn, ask_sha, ask, decision):
    state_key = behavior_state_key(decision)
    summary_15 = compact_words(ask, 15, 220)
    row = conn.execute(
        """
        SELECT state_key, summary_15
        FROM CHAT_STATE_SNAPSHOTS
        ORDER BY created_at DESC
        LIMIT 1
        """
    ).fetchone()
    conn.execute(
        """
        INSERT INTO CHAT_STATE_SNAPSHOTS (
            snapshot_id, ask_sha256, route, state_key, summary_15, tokens_json
        )
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        (
            unique_prefix("STATE", ask_sha),
            ask_sha,
            decision["route"],
            state_key,
            summary_15,
            json.dumps(decision["tokens"][:12], ensure_ascii=True),
        ),
    )
    if row:
        transition_id = f"TRANS::{row['state_key']}::{state_key}::{decision['route']}"
        conn.execute(
            """
            INSERT INTO CHAT_STATE_TRANSITIONS (
                transition_id, from_state, to_state, route, count, last_ask_sha256,
                sample_from_summary, sample_to_summary
            )
            VALUES (?, ?, ?, ?, 1, ?, ?, ?)
            ON CONFLICT(transition_id) DO UPDATE SET
                count = count + 1,
                last_ask_sha256 = excluded.last_ask_sha256,
                sample_from_summary = excluded.sample_from_summary,
                sample_to_summary = excluded.sample_to_summary,
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                transition_id,
                row["state_key"],
                state_key,
                decision["route"],
                ask_sha,
                row["summary_15"],
                summary_15,
            ),
        )
    return {
        "state_key": state_key,
        "summary_15": summary_15,
    }


def fetch_markov_hints(conn, state_key, route, ask_sha, limit=3):
    if not table_exists(conn, "CHAT_STATE_TRANSITIONS"):
        return []
    rows = conn.execute(
        """
        SELECT to_state, count, sample_to_summary, updated_at
        FROM CHAT_STATE_TRANSITIONS
        WHERE from_state = ? AND route = ?
        ORDER BY count DESC, updated_at DESC
        LIMIT 12
        """,
        (state_key, route),
    ).fetchall()
    if not rows:
        return []
    grouped = []
    for row in rows:
        grouped.append({
            "kind": "markov_hint",
            "weight": int(row["count"]),
            "summary_15": compact_words(
                f"Likely next intent: {row['sample_to_summary']}", 15, 220
            ),
            "state": row["to_state"],
            "reason": f"transition_count={row['count']}",
        })
    grouped.sort(key=lambda item: item["weight"], reverse=True)
    top_weight = grouped[0]["weight"]
    if all(item["weight"] == top_weight for item in grouped[: min(len(grouped), limit)]):
        rng = random.Random(ask_sha)
        rng.shuffle(grouped)
    return grouped[:limit]


def source_trust(source_name):
    if source_name.startswith("FULL_DB::"):
        table = source_name.split("::", 1)[1]
        return SOURCE_TRUST_WEIGHTS.get(table, 3)
    return SOURCE_TRUST_WEIGHTS.get(source_name, 4)


def route_fit(source_name, route):
    source = source_name.upper()
    if "INDUSTRY_RESEARCH" in source:
        return 5 if route in {"planning", "build", "chat"} else 3
    if route == "build" and any(term in source for term in ("CODE", "BLOCKCHAIN", "CANDIDATE", "QUEUE")):
        return 5
    if route == "planning" and any(term in source for term in ("TOPO", "TODO", "ACL", "GAME", "AGENT", "POLICY")):
        return 5
    if route == "chat" and any(term in source for term in ("CHAT", "USER", "RAG", "TRIPLET")):
        return 4
    return 1


def compress_source_card(item, purpose, route):
    data = item.get("data", {})
    summary = source_summary_text(data, item.get("source", "unknown"))
    card = {
        "source": item.get("source", "unknown"),
        "sha256": item.get("sha256", "")[:16],
        "score": item.get("score", 0),
        "trust": item.get("trust", source_trust(item.get("source", ""))),
        "route_fit": item.get("route_fit", route_fit(item.get("source", ""), route)),
        "compound_score": item.get("compound_score", item.get("score", 0)),
        "summary": compact_words(summary, 32, 300),
        "card_15": compact_words(summary, 15),
        "applicability": compact_words(f"Use for {purpose['action']} via {item.get('source', 'unknown')}", 15),
        "risk": "may be stale or keyword-only; verify before promotion",
    }
    return card


def rerank_and_compress(items, purpose, tokens, route, limit=12):
    enriched = []
    for index, item in enumerate(items):
        source_name = item.get("source", "")
        if route != "build" and any(term in source_name.upper() for term in ("CODE_BLOCKCHAIN", "LOGIC_BLOCKCHAIN_QUEUE", "BLOCKCHAIN_LEDGER")):
            item = dict(item)
            item["score"] = max(0, int(item.get("score", 0)) - 24)
        trust = source_trust(item.get("source", ""))
        fit = route_fit(item.get("source", ""), route)
        diversity = max(0, 4 - index // 4)
        density = min(6, len(json.dumps(item.get("data", {}), ensure_ascii=True)) // 180)
        compound = int(item.get("score", 0)) + trust + fit + diversity + density
        item = dict(item)
        item["trust"] = trust
        item["route_fit"] = fit
        item["compound_score"] = compound
        item["retrieval_epoch"] = "query_expand_hybrid_rerank_compress"
        item["card"] = compress_source_card(item, purpose, route)
        enriched.append(item)
    enriched.sort(key=lambda row: (row["compound_score"], row["trust"], row.get("score", 0)), reverse=True)

    selected = []
    seen_sources = {}
    for item in enriched:
        source = item.get("source", "unknown")
        count = seen_sources.get(source, 0)
        if count >= 3 and len(selected) >= 6:
            continue
        selected.append(item)
        seen_sources[source] = count + 1
        if len(selected) >= limit:
            break
    return selected


def build_nominal_context(user_profile):
    topology = user_profile.get("topology", {})
    frequent_terms = [row.get("term") for row in user_profile.get("frequent_terms", []) if row.get("term")]
    active_goals = []
    try:
        active_goals = json.loads(topology.get("active_goals_json", "[]"))
    except Exception:
        active_goals = []
    nominal_facts = {}
    for row in user_profile.get("nominal_facts", []):
        fact_type = row.get("fact_type", "general")
        nominal_facts.setdefault(fact_type, [])
        nominal_facts[fact_type].append(f"{row.get('fact_key')}: {row.get('fact_value')}")
    return {
        "condensed_context": compact_words(topology.get("condensed_context", ""), 28, 320),
        "active_goals": active_goals[:4],
        "frequent_terms": frequent_terms[:8],
        "nominal_facts": {
            key: values[:3]
            for key, values in nominal_facts.items()
        },
        "recent_summaries": [
            row.get("want_summary", "")
            for row in user_profile.get("recent_binomial_summaries", [])[:2]
            if row.get("want_summary")
        ],
    }


def collect_feedback_behavior_cards(conn, tokens):
    if not table_exists(conn, "RAG_MANIFOLD"):
        return []
    rows = conn.execute(
        """
        SELECT message, feedback_type, timestamp
        FROM RAG_MANIFOLD
        ORDER BY timestamp DESC, id DESC
        LIMIT 120
        """
    ).fetchall()
    grouped = {"like": [], "dislike": []}
    for row in rows:
        feedback_type = str(row["feedback_type"] or "").lower()
        if "dislike" in feedback_type:
            bucket = "dislike"
        elif "like" in feedback_type or "success" in feedback_type:
            bucket = "like"
        else:
            continue
        message = str(row["message"] or "")
        match_score = score_text(message, tokens)
        if not match_score and len(grouped[bucket]) >= 18:
            continue
        grouped[bucket].append({
            "message": message,
            "match_score": match_score,
            "timestamp": row["timestamp"],
        })
    cards = []
    for bucket, label in (("like", "liked intents"), ("dislike", "disliked intents")):
        items = grouped[bucket]
        if not items:
            continue
        items.sort(key=lambda item: item["match_score"], reverse=True)
        lead = [item["message"] for item in items[:3] if item["message"]]
        cards.append({
            "kind": f"feedback_{bucket}",
            "weight": sum(item["match_score"] for item in items[:3]) + min(8, len(items)),
            "summary_15": compact_words(f"{label}: {' ; '.join(lead)}", 15, 220),
            "reason": f"feedback_count={len(items)}",
        })
    return cards


def collect_related_behavior_cards(sources, code_sources, route):
    cards = []
    for item in (code_sources[:2] if route == "build" else []) + sources[:3]:
        card = item.get("card", {})
        cards.append({
            "kind": "related_db_logic",
            "weight": int(item.get("compound_score", item.get("score", 0))),
            "summary_15": card.get("card_15", ""),
            "reason": f"{item.get('source', 'unknown')} sha={item.get('sha256', '')[:12]}",
        })
    return cards


def choose_behavior_pack(ask_sha, feedback_cards, related_cards, markov_cards, limit=5):
    pool = [card for card in feedback_cards + related_cards + markov_cards if card.get("summary_15")]
    if not pool:
        return []
    pool.sort(key=lambda item: item.get("weight", 0), reverse=True)
    cutoff_weight = pool[min(len(pool), limit) - 1].get("weight", 0)
    top = [item for item in pool if item.get("weight", 0) > cutoff_weight]
    tied = [item for item in pool if item.get("weight", 0) == cutoff_weight]
    if len(top) >= limit:
        return top[:limit]
    remaining = limit - len(top)
    if len(tied) > remaining:
        rng = random.Random(ask_sha)
        rng.shuffle(tied)
    return top + tied[:remaining]


def evidence_sufficiency(cards, route):
    if not cards:
        return {
            "status": "insufficient",
            "confidence": 0.1,
            "reason": "no retrieval cards found",
        }
    trust_sum = sum(card.get("trust", 0) for card in cards)
    fit_sum = sum(card.get("route_fit", 0) for card in cards)
    confidence = min(0.96, 0.12 + trust_sum / 80 + fit_sum / 70)
    needed = 0.55 if route == "chat" else 0.7
    return {
        "status": "sufficient" if confidence >= needed else "needs_web_or_more_db",
        "confidence": round(confidence, 3),
        "reason": "computed from source trust, route fit, and card count",
    }


def web_snippet_plan(ask, decision, purpose, cards, sufficiency):
    tokens = decision["tokens"]
    return {
        "status": "queued_if_needed" if sufficiency["status"] != "sufficient" or decision["route"] != "chat" else "not_needed_for_direct_chat",
        "query": " ".join(tokens[:10]),
        "ask_card_15": compact_words(ask, 15),
        "purpose_card_15": compact_words(purpose["purpose"], 15),
        "required_format": [
            "claim",
            "source_url_or_local_path",
            "source_sha256",
            "applicability",
            "risk",
        ],
        "noise_policy": "discard marketing/duplicates; keep API contracts, standards, test commands, safety constraints",
    }


def merged_winner_architecture(decision, sufficiency):
    return {
        "name": "VIPER_GenAI_DB_Retrieval_Epoch",
        "winner_logic": "separate retrieval sidecar/API plus purpose-first Fabric lens",
        "patterns_merged": [
            "RAG: explicit DB/ledger memory grounds generation",
            "Self-RAG: retrieve only when useful and critique sufficiency",
            "RAGAS/ARES: evaluate context relevance, faithfulness, answer relevance",
            "Google GenAI DB Retrieval App: DB-backed retrieval service triggered by agent/tool flow",
            "VIPER: topological purpose cards, SHA-256 ledger, Karoo proposal gate, Java SDK persistence",
        ],
        "runtime_flow": [
            "classify route",
            "infer purpose",
            "rewrite/expand query",
            "retrieve local DB/ledger/success records",
            "trust+routing rerank",
            "compress to 15-word evidence cards",
            "check sufficiency",
            "queue web snippets only if needed",
            "send task directions to chat/build/planning",
            "log tests/eval/proof",
        ],
        "promotion_metrics": {
            "context_relevance": "retrieved cards match purpose",
            "faithfulness": "answer uses cards without inventing unsupported facts",
            "answer_relevance": "answer performs the requested chat/planning/build task",
            "latency": "avoid web/tool calls when DB sufficiency is high",
            "safety": "no raw data export; no GUI mutation without request",
        },
        "current_sufficiency": sufficiency,
        "next_vector_layer": "add BM25/vector/topology similarity behind the retrieval API when resources permit",
    }


def search_table(conn, table, columns, label, tokens, limit=24):
    if not table_exists(conn, table):
        return []
    safe_cols = ", ".join(columns)
    try:
        rows = conn.execute(f"SELECT {safe_cols} FROM {table} LIMIT 240").fetchall()
    except sqlite3.Error:
        return []
    results = []
    for row in rows:
        parts = []
        data = {}
        for col in columns:
            value = row[col] if col in row.keys() else None
            if value is None:
                continue
            text = str(value)
            data[col] = text[:700]
            parts.append(text)
        haystack = "\n".join(parts)
        score = score_text(haystack, tokens)
        if score:
            results.append({
                "source": label,
                "score": score,
                "data": data,
                "sha256": sha256_text(haystack),
            })
    results.sort(key=lambda item: item["score"], reverse=True)
    return results[:limit]


def generic_search_all_tables(conn, tokens, limit_per_table=2, max_tables=24):
    tables = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
    ).fetchall()
    results = []
    for table_row in tables[:max_tables]:
        table = table_row["name"]
        if table in GENERIC_SCAN_EXCLUDE:
            continue
        try:
            cols = conn.execute(f"PRAGMA table_info({table})").fetchall()
            col_names = [col["name"] for col in cols]
            if not col_names:
                continue
            select_cols = col_names[:8]
            safe_cols = ", ".join(select_cols)
            rows = conn.execute(f"SELECT {safe_cols} FROM {table} LIMIT 80").fetchall()
        except sqlite3.Error:
            continue
        table_hits = []
        for row in rows:
            data = {}
            parts = []
            for col in select_cols:
                value = row[col] if col in row.keys() else None
                if value is None:
                    continue
                text = str(value)
                data[col] = text[:500]
                parts.append(text)
            haystack = "\n".join(parts)
            score = score_text(haystack, tokens)
            if score:
                table_hits.append({
                    "source": f"FULL_DB::{table}",
                    "score": score,
                    "data": data,
                    "sha256": sha256_text(haystack),
                })
        table_hits.sort(key=lambda item: item["score"], reverse=True)
        results.extend(table_hits[:limit_per_table])
    results.sort(key=lambda item: item["score"], reverse=True)
    return results[:24]


def industry_research_cards(tokens, route):
    joined = " ".join(tokens).lower()
    wants_research = any(term in joined for term in (
        "rag", "retrieval", "retreival", "research", "industry", "standard",
        "standards", "database", "db", "agent", "react", "web"
    ))
    if not wants_research:
        return []
    cards = [
        {
            "source": "INDUSTRY_RESEARCH_NOTES",
            "score": score_text("RAG combines parametric model memory with explicit non-parametric retrieved memory and improves factuality", tokens) + 40,
            "data": {
                "claim": "Use retrieval to ground generation in explicit external memory instead of relying only on model weights.",
                "source_url": "https://arxiv.org/abs/2005.11401",
                "applicability": "VIPER DB/ledger acts as non-parametric memory.",
                "risk": "Current local version lacks vector embeddings; use hybrid keyword/trust until vector layer exists.",
            },
            "sha256": sha256_text("RAG Lewis 2020 explicit non-parametric memory"),
        },
        {
            "source": "INDUSTRY_RESEARCH_NOTES",
            "score": score_text("Self-RAG adaptively retrieve generate critique reflection factuality relevance", tokens) + 40,
            "data": {
                "claim": "Retrieve only when useful, then critique relevance and factuality before final generation.",
                "source_url": "https://arxiv.org/abs/2310.11511",
                "applicability": "VIPER sufficiency check decides whether local DB is enough or web/research is needed.",
                "risk": "Local critique is deterministic for now; model judge can be added later.",
            },
            "sha256": sha256_text("Self-RAG adaptive retrieval critique"),
        },
        {
            "source": "INDUSTRY_RESEARCH_NOTES",
            "score": score_text("RAGAS context relevance faithfulness answer relevance evaluation", tokens) + 40,
            "data": {
                "claim": "Evaluate RAG with context relevance, answer faithfulness, and answer relevance.",
                "source_url": "https://arxiv.org/abs/2309.15217",
                "applicability": "VIPER tests should log whether retrieved cards were relevant and faithfully used.",
                "risk": "Metrics are approximated until a judge/eval runner is wired.",
            },
            "sha256": sha256_text("RAGAS context relevance faithfulness answer relevance"),
        },
        {
            "source": "INDUSTRY_RESEARCH_NOTES",
            "score": score_text("Google Cloud GenAI database retrieval app RAG ReACT separate retrieval service SQL vector security latency cost", tokens) + 44,
            "data": {
                "claim": "Run retrieval as a separate service/API, use DB precision plus semantic similarity, and gate by security, scale, quality, latency, and cost.",
                "source_url": "https://cloud.google.com/blog/products/databases/introducing-sample-genai-databases-retrieval-app",
                "applicability": "VIPER should keep the Java SDK/bridge as orchestration and the retrieval sidecar as a separate service.",
                "risk": "Cloud/vector pieces are future layers; local SQLite must remain conservative and auditable.",
            },
            "sha256": sha256_text("Google GenAI Databases Retrieval App RAG ReACT service API"),
        },
        {
            "source": "INDUSTRY_RESEARCH_NOTES",
            "score": score_text("LangChain retrieval 2-step agentic hybrid RAG knowledge base retrieval pipeline", tokens) + 36,
            "data": {
                "claim": "Choose retrieval architecture by task: simple two-step for predictable queries, agentic/hybrid for tool-heavy tasks.",
                "source_url": "https://docs.langchain.com/oss/python/langchain/retrieval",
                "applicability": "VIPER route decides chat/planning/build retrieval depth.",
                "risk": "Agentic retrieval can increase latency; use sufficiency gates.",
            },
            "sha256": sha256_text("LangChain retrieval architectures"),
        },
    ]
    return cards


def search_database(conn, tokens, route):
    sources = []
    sources.extend(industry_research_cards(tokens, "unknown"))
    sources.extend(search_table(conn, "USER_TOPOLOGY_PROFILE", [
        "profile_id", "condensed_context", "preferences_json", "active_goals_json", "predictive_terms_json"
    ], "USER_TOPOLOGY_PROFILE", tokens, limit=4))
    sources.extend(search_table(conn, "USER_NOMINAL_FACTS", [
        "fact_key", "fact_value", "fact_type", "source_excerpt"
    ], "USER_NOMINAL_FACTS", tokens, limit=6))
    if route != "build":
        sources.extend(search_table(conn, "CHAT_MEMORY", [
            "id", "user_message", "ai_response", "timestamp"
        ], "CHAT_MEMORY", tokens, limit=8))
    sources.extend(search_table(conn, "TRIPLET_MANIFOLD", ["id", "type", "label", "description"], "TRIPLET_MANIFOLD", tokens))
    sources.extend(search_table(conn, "RAG_MANIFOLD", ["id", "message", "feedback_type", "timestamp"], "RAG_MANIFOLD", tokens))
    sources.extend(search_table(conn, "TOPO_CHUNKS", ["id", "subsystem_id", "symbol", "source_path", "metadata_json"], "TOPO_CHUNKS", tokens))
    sources.extend(search_table(conn, "TOPO_APPROVAL_REPORTS", ["id", "subsystem_id", "summary", "status"], "TOPO_APPROVAL_REPORTS", tokens))
    sources.extend(search_table(conn, "GLOBAL_TODO_QUEUE", ["todo_id", "title", "details", "status"], "GLOBAL_TODO_QUEUE", tokens))
    sources.extend(search_table(conn, "GLOBAL_ACL_MESSAGES", ["message_id", "sender", "receiver", "performative", "content"], "GLOBAL_ACL_MESSAGES", tokens))
    if route != "build":
        sources.extend(search_table(conn, "GAME_DATA", ["game_id", "data_type", "payload_json", "status"], "GAME_DATA", tokens))
    sources.extend(search_table(conn, "WEBCRAWL_RESEARCH_REQUESTS", [
        "request_id", "route", "query_json", "noise_policy_json", "status"
    ], "WEBCRAWL_RESEARCH_REQUESTS", tokens, limit=6))
    sources.extend(search_table(conn, "SYSTEM_TEST_LOG", [
        "id", "test_name", "layer", "status", "details", "evidence_json"
    ], "SYSTEM_TEST_LOG", tokens, limit=6))
    sources.extend(search_table(conn, "KAROO_DISTILLATION_QUEUE", [
        "queue_id", "source_kind", "route", "summary_text", "status"
    ], "KAROO_DISTILLATION_QUEUE", tokens, limit=6))
    sources.extend(search_table(conn, "BENCHMARK_EVENTS", [
        "benchmark_id", "component", "operation", "route", "status", "details_json"
    ], "BENCHMARK_EVENTS", tokens, limit=6))
    if route == "build":
        if len(sources) < 10:
            sources.extend(generic_search_all_tables(conn, tokens, limit_per_table=1, max_tables=10))
    elif route == "planning":
        if len(sources) < 14:
            sources.extend(generic_search_all_tables(conn, tokens, limit_per_table=2, max_tables=18))
    else:
        if len(sources) < 12:
            sources.extend(generic_search_all_tables(conn, tokens, limit_per_table=2, max_tables=14))
    sources.sort(key=lambda item: item["score"], reverse=True)
    return sources[:48]


def search_successful_code(conn, tokens):
    sources = []
    sources.extend(search_table(conn, "SUCCESSFUL_CODE_ADVANCES", [
        "advance_id", "source_kind", "source_id", "route", "summary_text", "status"
    ], "SUCCESSFUL_CODE_ADVANCES", tokens, limit=10))
    sources.extend(search_table(conn, "CODE_BLOCKCHAIN_DB", [
        "code_block_id", "source_queue_id", "payload_sha256", "chain_hash", "payload_json", "storage_role"
    ], "CODE_BLOCKCHAIN_DB_SUCCESS", tokens, limit=12))
    sources.extend(search_table(conn, "BLOCKCHAIN_LEDGER", [
        "id", "block_hash", "prev_hash", "data", "timestamp"
    ], "BLOCKCHAIN_LEDGER_SUCCESS", tokens, limit=12))
    if table_exists(conn, "LOGIC_BLOCKCHAIN_QUEUE"):
        try:
            rows = conn.execute("""
                SELECT id, payload_sha256, chain_hash, payload_json, status, attempts
                FROM LOGIC_BLOCKCHAIN_QUEUE
                WHERE status = 'shipped'
                LIMIT 200
            """).fetchall()
            for row in rows:
                haystack = "\n".join(str(row[col]) for col in row.keys())
                score = score_text(haystack, tokens)
                if score:
                    sources.append({
                        "source": "LOGIC_BLOCKCHAIN_QUEUE_SHIPPED",
                        "score": score,
                        "data": {col: str(row[col])[:700] for col in row.keys()},
                        "sha256": sha256_text(haystack),
                    })
        except sqlite3.Error:
            pass
    sources.extend(search_table(conn, "TOPO_CANDIDATES", [
        "id", "experiment_id", "chunk_id", "candidate_sha256", "comparison_count", "confidence", "action", "report"
    ], "KAROO_CANDIDATES", tokens, limit=12))
    filtered = []
    for item in sources:
        data_blob = json.dumps(item["data"], sort_keys=True).lower()
        if any(marker in data_blob for marker in ["shipped", "success", "pass", "approved", "candidate", "logic_block"]):
            item["score"] += 4
            filtered.append(item)
    filtered.sort(key=lambda item: item["score"], reverse=True)
    return filtered[:10]


def noise_policy(route):
    return {
        "route": route,
        "webcrawl": "logical_summary_only",
        "discard": [
            "ads",
            "marketing",
            "duplicate snippets",
            "unverified claims",
            "style-only variants unless comparing syntax",
            "content without license/provenance signal",
        ],
        "keep": [
            "API contracts",
            "compile/test commands",
            "minimal working examples",
            "security constraints",
            "performance notes",
            "project-local successful code hashes",
        ],
        "reduction": "summarize to claims + source hash + applicability + risk",
    }


def template_hooks(route):
    return {
        "database_hooks": [
            "TRIPLET_MANIFOLD",
            "RAG_MANIFOLD",
            "TOPO_CHUNKS",
            "TOPO_APPROVAL_REPORTS",
            "GLOBAL_TODO_QUEUE",
            "GAME_DATA",
        ],
        "programming_success_hooks": [
            "CODE_BLOCKCHAIN_DB",
            "BLOCKCHAIN_LEDGER",
            "LOGIC_BLOCKCHAIN_QUEUE(status=shipped)",
            "TOPO_CANDIDATES",
        ] if route == "build" else [],
        "webcrawl_hooks": [
            "WEBCRAWL_RESEARCH_REQUESTS",
            "approved external docs only",
            "summarize and reduce noise before model injection",
        ],
        "onedrive_slow_pipeline": "hash summaries and approved artifacts only",
    }


def fabric_layer_for_route(route):
    if route == "build":
        return "programming"
    if route == "chat":
        return "chat"
    return "generalist"


def fetch_user_profile(conn):
    profile = {}
    if table_exists(conn, "USER_TOPOLOGY_PROFILE"):
        row = conn.execute(
            """
            SELECT chat_count, condensed_context, preferences_json,
                   active_goals_json, predictive_terms_json, instructions_json,
                   profile_sha256, updated_at
            FROM USER_TOPOLOGY_PROFILE
            WHERE profile_id = 'VIPER_USER_TOPOLOGY_V1'
            """
        ).fetchone()
        if row:
            profile["topology"] = {key: row[key] for key in row.keys()}
    if table_exists(conn, "CONVERSATION_BINOMIAL_SUMMARY"):
        rows = conn.execute(
            """
            SELECT want_summary, action_summary, summary_sha256, created_at
            FROM CONVERSATION_BINOMIAL_SUMMARY
            ORDER BY created_at DESC
            LIMIT 3
            """
        ).fetchall()
        profile["recent_binomial_summaries"] = [dict(row) for row in rows]
    if table_exists(conn, "USER_WORD_STATS"):
        rows = conn.execute(
            """
            SELECT term, count, route_hits_json
            FROM USER_WORD_STATS
            ORDER BY count DESC, last_seen_at DESC
            LIMIT 24
            """
        ).fetchall()
        profile["frequent_terms"] = [dict(row) for row in rows]
    if table_exists(conn, "USER_NOMINAL_FACTS"):
        rows = conn.execute(
            """
            SELECT fact_key, fact_value, fact_type, confidence, source_excerpt, updated_at
            FROM USER_NOMINAL_FACTS
            ORDER BY updated_at DESC
            LIMIT 12
            """
        ).fetchall()
        profile["nominal_facts"] = [dict(row) for row in rows]
    return profile


def update_user_prediction_tables(conn, ask, decision, ask_sha):
    tokens = tokenize(ask, 24)
    if not tokens:
        return
    wants = {
        "preserve_gui": ("gui" in tokens or "webpage" in tokens or "java" in tokens),
        "real_tiny_models": any(term in tokens for term in ("qwen", "smollm", "danube", "tiny", "model")),
        "database_retrieval": any(term in tokens for term in ("database", "retrieval", "retreival", "db", "data")),
        "agent_network": any(term in tokens for term in ("agent", "agents", "network", "sync", "nas")),
        "rolling_triplet": any(term in tokens for term in ("rolling", "triplet", "recursive", "karoo")),
        "benchmarking": any(term in tokens for term in ("benchmark", "test", "proof", "graphs")),
    }
    route = decision["route"]
    placeholders = ", ".join("?" for _ in tokens)
    existing_hits = {}
    if placeholders:
        rows = conn.execute(
            f"SELECT term, route_hits_json FROM USER_WORD_STATS WHERE term IN ({placeholders})",
            tokens,
        ).fetchall()
        for row in rows:
            try:
                existing_hits[row["term"]] = json.loads(row["route_hits_json"] or "{}")
            except Exception:
                existing_hits[row["term"]] = {}
    stat_rows = []
    for term in tokens:
        route_hits = dict(existing_hits.get(term, {}))
        route_hits[route] = int(route_hits.get(route, 0)) + 1
        stat_rows.append((term, json.dumps(route_hits, ensure_ascii=True, sort_keys=True)))
    conn.executemany(
        """
        INSERT INTO USER_WORD_STATS (term, count, route_hits_json, last_seen_at)
        VALUES (?, 1, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(term) DO UPDATE SET
            count = count + 1,
            route_hits_json = excluded.route_hits_json,
            last_seen_at = CURRENT_TIMESTAMP
        """,
        stat_rows,
    )
    for want_key, active in wants.items():
        if not active:
            continue
        conn.execute(
            """
            INSERT INTO USER_TOPOLOGICAL_WANTS (
                want_key, count, last_ask_sha256, last_route, last_seen_at
            )
            VALUES (?, 1, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(want_key) DO UPDATE SET
                count = count + 1,
                last_ask_sha256 = excluded.last_ask_sha256,
                last_route = excluded.last_route,
                last_seen_at = CURRENT_TIMESTAMP
            """,
            (want_key, ask_sha, route),
        )


def default_markov_state(ask, decision):
    return {
        "state_key": behavior_state_key(decision),
        "summary_15": compact_words(ask, 15, 220),
        "transition_from": None,
        "transition_count": 0,
    }


def should_use_tiny_runtime_for_lens(ask, decision, sources, code_sources):
    if not HAS_TINY_RUNTIME:
        return False, "tiny_runtime_unavailable"
    lower = str(ask or "").lower()
    explicit_model_terms = (
        "qwen", "smollm", "danube", "deepseek", "distill", "distilled",
        "llm", "model", "models", "retrieval matcher", "tiny runtime",
    )
    if any(term in lower for term in explicit_model_terms):
        candidate_count = len(sources) + len(code_sources)
        if candidate_count <= 18:
            return True, "explicit_model_request"
        return False, "candidate_budget_guardrail"
    if decision["route"] == "planning" and any(term in lower for term in ("compare", "winner", "merge")):
        candidate_count = len(sources) + len(code_sources)
        if candidate_count <= 12:
            return True, "comparison_request"
        return False, "candidate_budget_guardrail"
    return False, "latency_guardrail"


def build_dynamic_template(route, token_limit):
    hooks = template_hooks(route)
    policy = noise_policy(route)
    fabric_layer = fabric_layer_for_route(route)
    if route == "build":
        mode = (
            "PROGRAMMING TEMPLATE: retrieve successful code first, then webcrawl for "
            "missing facts, reduce to tested claims, compare with Karoo, and return "
            "a compile-ready proposal."
        )
    elif route == "planning":
        mode = (
            "PLANNING TEMPLATE: retrieve topology and SOPs first, webcrawl only for "
            "current facts, reduce to decision points, then propose gates."
        )
    else:
        mode = (
            "CHAT TEMPLATE: answer directly with generous room, but keep DB/crawl "
            "noise out unless needed."
        )
    return "\n".join([
        "DYNAMIC VIPER FABRIC TEMPLATE",
        f"route: {route}",
        f"fabric_layer: {fabric_layer}",
        f"token_budget: {token_limit}",
        mode,
        "",
        "HOOKS:",
        json.dumps(hooks, ensure_ascii=True, indent=2),
        "",
        "NOISE REDUCTION:",
        json.dumps(policy, ensure_ascii=True, indent=2),
    ]), hooks, policy


def local_fabric_hint(route):
    if FABRIC_SOURCE.exists():
        return {
            "status": "available_local_archive",
            "path": str(FABRIC_SOURCE),
            "mode": "concept-compatible; no template mutation",
        }
    return {
        "status": "not_found",
        "path": str(FABRIC_SOURCE),
        "mode": "using built-in minimal lens templates",
    }


def build_lens(
    ask,
    decision,
    sources,
    code_sources,
    purpose,
    web_plan,
    sufficiency,
    winner,
    tiny_card,
    retrieval_match,
    qwen_lens,
    rolling_card,
    fabric_layer,
    nominal_context,
    behavior_pack,
    markov_state,
):
    route = decision["route"]
    token_limit = decision["token_limit"]
    fabric_hint = local_fabric_hint(route)
    template_text, hooks, policy = build_dynamic_template(route, token_limit)
    db_cards = [item["card"] for item in sources[:8] if "card" in item]
    code_cards = [item["card"] for item in code_sources[:5] if "card" in item]
    if route == "chat":
        contract = (
            "CHAT ROUTE: answer directly. Do not invoke Karoo. Do not propose file "
            "edits unless the user asks for action. Keep it warm and useful. "
            "Use the PURPOSE and DB cards to maintain logical presence without dumping raw data."
        )
        source_lines = [
            f"Retrieval ran and compressed {len(db_cards)} DB cards. "
            "Use only cards that directly help the answer."
        ]
    elif route == "planning":
        contract = (
            "PLANNING ROUTE: retrieve topology/SOPs, optionally queue webcrawl "
            "research, reduce noise to decisions, then run rolling recursive "
            "planning with approval gates. Karoo stays proposal-only."
        )
        source_lines = []
    else:
        contract = (
            "BUILD ROUTE: pull successful code from Karoo DB and SHA ledger first. "
            "Abliterated queues webcrawl/code suggestions only for missing context. "
            "Karoo compares each advancement against three options, logs actor-critic "
            "stop/best decisions, then returns the compile-ready proposal path to "
            "chat. No self-mutation outside the 99.99% + 10% gate."
        )
        source_lines = []

    if route != "chat":
        if code_sources:
            source_lines.append("SUCCESSFUL CODE / LEDGER HOOKS:")
            for i, source in enumerate(code_sources[:5], start=1):
                data = source["data"]
                compact = " | ".join(f"{k}={str(v)[:120]}" for k, v in data.items())
                source_lines.append(
                    f"C{i}. {source['source']} compound={source.get('compound_score', source['score'])} "
                    f"trust={source.get('trust')} fit={source.get('route_fit')} sha={source['sha256'][:12]} :: {compact}"
                )
        for i, source in enumerate(sources[:5], start=1):
            data = source["data"]
            compact = " | ".join(f"{k}={str(v)[:120]}" for k, v in data.items())
            source_lines.append(
                f"{i}. {source['source']} compound={source.get('compound_score', source['score'])} "
                f"trust={source.get('trust')} fit={source.get('route_fit')} sha={source['sha256'][:12]} :: {compact}"
            )
        if not source_lines:
            source_lines.append("No strong DB matches. Use current ask only and log uncertainty.")

    return "\n".join([
        "VIPER FABRIC LENS",
        f"route: {route}",
        f"fabric_layer: {fabric_layer}",
        f"token_limit: {token_limit}",
        f"fabric_source: {fabric_hint['status']} ({fabric_hint['mode']})",
        f"template_sha256: {sha256_text(template_text)}",
        f"tiny_runtime: {'real_qwen_smollm' if HAS_TINY_RUNTIME else 'unavailable'}",
        "",
        "PURPOSE:",
        json.dumps(purpose, ensure_ascii=True, indent=2),
        "",
        "ACTIVE_QWEN_CHOOSER_LENS_100_WORDS:",
        qwen_lens.get("text", ""),
        "",
        "AXIOMATIC_RETRIEVAL_MATCH_50_WORDS:",
        retrieval_match.get("text", ""),
        "",
        "ROLLING_RECURSIVE_TRIPLET_CARD:",
        rolling_card.get("text", ""),
        "",
        "NOMINAL_USER_CONTEXT:",
        json.dumps(nominal_context, ensure_ascii=True, indent=2)[:900],
        "",
        "BEHAVIORAL_CONTEXT_PACK_TOP_5:",
        json.dumps({
            "current_state": markov_state,
            "cards": behavior_pack,
        }, ensure_ascii=True, indent=2)[:1600],
        "",
        "RETRIEVAL_EPOCH:",
        "purpose -> real_DB_retrieval -> SmolLM2_axiom_match -> Qwen2.5_lens -> route_response_or_task",
        "",
        "QUERY_VARIANTS:",
        "\n".join(f"- {variant}" for variant in decision.get("query_variants", [])[:6]),
        "",
        "DB_RETRIEVAL_CARDS:",
        json.dumps({
            "sufficiency": sufficiency,
            "logic_cards": db_cards,
            "successful_code_cards": code_cards,
        }, ensure_ascii=True, indent=2)[:3600],
        "",
        "WEB_SNIPPET_PLAN:",
        json.dumps(web_plan, ensure_ascii=True, indent=2),
        "",
        "MERGED_WINNER_ARCHITECTURE:",
        json.dumps(winner, ensure_ascii=True, indent=2),
        "",
        "TINY_PROMPT_ENGINEER_CARD_50_WORDS:",
        tiny_card,
        "",
        "TASK_DIRECTIONS:",
        "- Read ACTIVE_QWEN_CHOOSER_LENS first and answer/act toward that purpose.",
        "- Use DB_RETRIEVAL_CARDS as the grounded evidence packet.",
        "- Use BEHAVIORAL_CONTEXT_PACK_TOP_5 as compact conversation context; do not crowd the answer with it.",
        "- Use AXIOMATIC_RETRIEVAL_MATCH as the closest 50-word context card.",
        "- Use ROLLING_RECURSIVE_TRIPLET_CARD to coordinate Qwen/Karoo/abliterated passes.",
        "- If sufficiency is low, say what evidence is missing and use WEB_SNIPPET_PLAN.",
        "- For task requests, perform the smallest useful step and log proof.",
        "- For chat, keep the answer natural but grounded by the cards.",
        "",
        contract,
        "",
        "ASK_SHA256:",
        sha256_text(ask),
        "",
        "TOP TOKENS:",
        ", ".join(decision["tokens"][:24]),
        "",
        "MATCHED LOGIC SOURCES:",
        "\n".join(source_lines),
        "",
        "ACTIVE TEMPLATE:",
        template_text[:1800],
        "",
        "OUTPUT RULES:",
        "- Use exactly one lens for this chat turn.",
        "- Do not answer PASS/OK/DONE unless the user explicitly requests a verdict.",
        "- Preserve the locked Java/Three.js GUI unless the user explicitly asks.",
        "- For build work: one changed variable per test; end-to-end proof required.",
        "- For web research: crawl/log separately, then inject only reduced claims.",
        "- For long asks: use generous budget, summarize intent, then answer the highest-impact part.",
    ])


def craft_lens(ask):
    ask_sha = sha256_text(ask)
    event_id = unique_prefix("RET", ask_sha)
    lens_id = unique_prefix("LENS", ask_sha)
    epoch_id = unique_prefix("KAROO_EPOCH", ask_sha)
    decision = classify_ask(ask)
    if is_short_chat_fast_path(ask):
        lens = "\n".join([
            "VIPER FABRIC LENS",
            "route: chat",
            "fabric_layer: chat",
            f"token_limit: {decision['token_limit']}",
            "",
            "CHAT ROUTE: short direct fast-path.",
            "Answer directly without heavy retrieval, Karoo, or webcrawl.",
        ])
        return {
            "event_id": event_id,
            "lens_id": lens_id,
            "ask_sha256": ask_sha,
            "route": "chat",
            "fabric_layer": "chat",
            "token_limit": decision["token_limit"],
            "result_count": 0,
            "code_result_count": 0,
            "template_id": None,
            "axiomatic_match_id": None,
            "triplet_id": None,
            "purpose": {
                "purpose": "answer a short direct chat turn and keep the bridge responsive",
                "action": "commence_chat",
                "route": "chat",
                "ask_card_15": compact_words(ask, 15),
                "success_criteria": ["fast direct short-chat reply"],
            },
            "evidence_sufficiency": {"status": "sufficient", "confidence": 1.0, "reason": "short-chat fast-path"},
            "web_snippet_plan": {"status": "not_needed_for_direct_chat"},
            "merged_winner_architecture": {"name": "short_chat_fast_path"},
            "tiny_prompt_engineer_card_50_words": "Respond with a short direct conversational reply.",
            "nominal_context": {},
            "behavior_pack": [],
            "markov_state": {"state_key": "chat::fast_short_turn", "summary_15": compact_words(ask, 15)},
            "axiomatic_retrieval_match_50_words": {"text": "Short-chat fast-path.", "status": "fast_path", "meta": {}},
            "qwen_chooser_lens_100_words": {"text": "Short-chat fast-path.", "status": "fast_path", "meta": {}},
            "rolling_recursive_triplet_card": {"text": "No triplet escalation needed for short chat.", "status": "fast_path", "meta": {}},
            "tiny_model_status": {"enabled": HAS_TINY_RUNTIME, "mode": "short_chat_fast_path"},
            "lens": lens,
            "lens_sha256": sha256_text(lens),
        }
    if is_infra_build_fast_path(ask, decision):
        lens = "\n".join([
            "VIPER FABRIC LENS",
            "route: build",
            "fabric_layer: programming",
            "token_limit: 768",
            "",
            "BUILD FAST LANE: infrastructure-control request.",
            "Use runtime DB status, Karoo counters, and distilled success memory first.",
            "Skip the full broad retrieval sweep for Karoo/DB/speed control asks.",
        ])
        return {
            "event_id": event_id,
            "lens_id": lens_id,
            "ask_sha256": ask_sha,
            "route": "build",
            "fabric_layer": "programming",
            "token_limit": 768,
            "result_count": 0,
            "code_result_count": 0,
            "template_id": None,
            "axiomatic_match_id": None,
            "triplet_id": None,
            "purpose": {
                "purpose": "accelerate infrastructure-control build requests without a full DB sweep",
                "action": "perform_task",
                "route": "build",
                "ask_card_15": compact_words(ask, 15),
                "success_criteria": ["fast infrastructure control reply", "karoo remains proposal-only"],
            },
            "evidence_sufficiency": {"status": "sufficient", "confidence": 0.94, "reason": "infra build fast-path"},
            "web_snippet_plan": {"status": "not_needed_for_infra_control"},
            "merged_winner_architecture": {"name": "infra_build_fast_path"},
            "tiny_prompt_engineer_card_50_words": "Use DB runtime counters and distilled success memory first.",
            "nominal_context": {},
            "behavior_pack": [],
            "markov_state": {"state_key": "build::infra_control_fast", "summary_15": compact_words(ask, 15)},
            "axiomatic_retrieval_match_50_words": {"text": "Use runtime DB counters and distilled successes first.", "status": "fast_path", "meta": {}},
            "qwen_chooser_lens_100_words": {"text": "Fast build lane for Karoo/DB control request.", "status": "fast_path", "meta": {}},
            "rolling_recursive_triplet_card": {"text": "Run Karoo compare lanes concurrently with approval gate.", "status": "fast_path", "meta": {}},
            "tiny_model_status": {"enabled": HAS_TINY_RUNTIME, "mode": "infra_build_fast_path"},
            "lens": lens,
            "lens_sha256": sha256_text(lens),
        }
    decision["query_variants"] = query_variants(ask, decision["tokens"], decision["route"])
    fabric_layer = fabric_layer_for_route(decision["route"])
    purpose = infer_purpose(ask, decision)
    markov_state = default_markov_state(ask, decision)
    expanded_tokens = tokenize(" ".join(decision["query_variants"]), 64)
    sources = []
    code_sources = []
    user_profile = {}
    nominal_context = {}
    feedback_cards = []
    markov_cards = []
    related_cards = []
    behavior_pack = []
    try:
        with connect_db() as conn:
            ensure_migrated(conn)
            try:
                update_user_prediction_tables(conn, ask, decision, ask_sha)
                markov_state = record_markov_state(conn, ask_sha, ask, decision)
                conn.commit()
            except sqlite3.Error:
                conn.rollback()
            sources = search_database(conn, expanded_tokens, decision["route"])
            sources = rerank_and_compress(sources, purpose, expanded_tokens, decision["route"], limit=16)
            code_sources = search_successful_code(conn, expanded_tokens) if decision["route"] == "build" else []
            code_sources = rerank_and_compress(code_sources, purpose, expanded_tokens, decision["route"], limit=8)
            user_profile = fetch_user_profile(conn)
            nominal_context = build_nominal_context(user_profile)
            feedback_cards = collect_feedback_behavior_cards(conn, expanded_tokens)
            markov_cards = fetch_markov_hints(conn, markov_state["state_key"], decision["route"], ask_sha, limit=3)
    except sqlite3.Error:
        pass
    related_cards = collect_related_behavior_cards(sources, code_sources, decision["route"])
    behavior_pack = choose_behavior_pack(ask_sha, feedback_cards, related_cards, markov_cards, limit=5)
    cards = [item["card"] for item in sources[:8] if "card" in item]
    sufficiency = evidence_sufficiency(cards, decision["route"])
    web_plan = web_snippet_plan(ask, decision, purpose, cards, sufficiency)
    winner = merged_winner_architecture(decision, sufficiency)
    tiny_card = tiny_prompt_engineer_card(purpose, decision, web_plan)
    use_tiny_runtime, tiny_reason = should_use_tiny_runtime_for_lens(ask, decision, sources, code_sources)
    if use_tiny_runtime:
        retrieval_match = axiomatic_retrieval_match(
            ask,
            decision["route"],
            purpose,
            (code_sources if decision["route"] == "build" else []) + sources,
        )
        qwen_lens = qwen_choose_lens(
            ask,
            decision["route"],
            fabric_layer,
            decision["token_limit"],
            purpose,
            retrieval_match,
            sources,
            code_sources,
            web_plan,
            user_profile=user_profile,
        )
        rolling_card = qwen_rolling_triplet_card(
            ask,
            decision["route"],
            qwen_lens,
            retrieval_match,
        )
        tiny_status = tiny_model_status()
        tiny_status["mode"] = tiny_reason
    else:
        retrieval_match = {
            "text": deterministic_match_summary((code_sources if decision["route"] == "build" else []) + sources),
            "status": tiny_reason,
            "meta": {"reason": tiny_reason},
        }
        qwen_lens = {
            "text": compact_words(tiny_card, 100, 900),
            "status": tiny_reason,
            "meta": {"reason": tiny_reason},
        }
        rolling_card = {
            "text": "Use deterministic lens cards and keep Karoo proposal-only while latency guardrails are active.",
            "status": tiny_reason,
            "meta": {"reason": tiny_reason},
        }
        tiny_status = {"enabled": HAS_TINY_RUNTIME, "mode": tiny_reason}

    lens = build_lens(
        ask,
        decision,
        sources,
        code_sources,
        purpose,
        web_plan,
        sufficiency,
        winner,
        tiny_card,
        retrieval_match,
        qwen_lens,
        rolling_card,
        fabric_layer,
        nominal_context,
        behavior_pack,
        markov_state,
    )
    lens_sha = sha256_text(lens)
    template_text, hooks, policy = build_dynamic_template(decision["route"], decision["token_limit"])
    template_id = unique_prefix("FABT", ask_sha)
    match_id = unique_prefix("AXMATCH", ask_sha)
    triplet_id = unique_prefix("ROLLTRIP", ask_sha)
    try:
        with connect_db() as conn:
            ensure_migrated(conn)
            conn.execute(
                """
                INSERT INTO AXIOMATIC_RETRIEVAL_MATCHES (
                    match_id, ask_sha256, route, fabric_layer, match_text, status,
                    candidate_count, model_meta_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    match_id,
                    ask_sha,
                    decision["route"],
                    fabric_layer,
                    retrieval_match.get("text", ""),
                    retrieval_match.get("status", "unknown"),
                    len(sources) + len(code_sources),
                    json.dumps(retrieval_match.get("meta", {}), ensure_ascii=True, sort_keys=True),
                ),
            )
            conn.execute(
                """
                INSERT INTO ROLLING_TRIPLET_RUNS (
                    triplet_id, ask_sha256, route, fabric_layer, chooser_lens_text,
                    retrieval_match_text, rolling_card_text, status, meta_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    triplet_id,
                    ask_sha,
                    decision["route"],
                    fabric_layer,
                    qwen_lens.get("text", ""),
                    retrieval_match.get("text", ""),
                    rolling_card.get("text", ""),
                    rolling_card.get("status", "unknown"),
                    json.dumps({
                        "chooser": qwen_lens.get("meta", {}),
                        "retrieval": retrieval_match.get("meta", {}),
                        "rolling": rolling_card.get("meta", {}),
                        "tiny_status": tiny_status,
                    }, ensure_ascii=True, sort_keys=True),
                ),
            )
            for role, item in (
                ("retrieval_matcher", retrieval_match),
                ("qwen_chooser", qwen_lens),
                ("qwen_rolling_triplet", rolling_card),
            ):
                meta = item.get("meta", {})
                conn.execute(
                    """
                    INSERT INTO TINY_MODEL_EVENTS (
                        event_id, ask_sha256, model_role, status, details_json
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (
                        unique_prefix("TINYEVT", ask_sha),
                        ask_sha,
                        role,
                        item.get("status", "unknown"),
                        json.dumps(meta, ensure_ascii=True, sort_keys=True),
                    ),
                )
                conn.execute(
                    """
                    INSERT INTO BENCHMARK_EVENTS (
                        benchmark_id, component, operation, route, duration_ms, status, details_json
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        unique_prefix("BENCH", ask_sha),
                        "tiny_model_runtime",
                        role,
                        decision["route"],
                        int(meta.get("duration_ms", 0) or 0),
                        item.get("status", "unknown"),
                        json.dumps(meta, ensure_ascii=True, sort_keys=True),
                    ),
                )
            conn.execute(
                """
                INSERT INTO FABRIC_TEMPLATE_SNAPSHOTS (
                    template_id, route, ask_sha256, template_text, template_sha256, hooks_json, noise_policy_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    template_id,
                    decision["route"],
                    ask_sha,
                    template_text,
                    sha256_text(template_text),
                    json.dumps(hooks, ensure_ascii=True, sort_keys=True),
                    json.dumps(policy, ensure_ascii=True, sort_keys=True),
                ),
            )
            conn.execute(
                """
                INSERT INTO FABRIC_LENSES (
                    lens_id, route, ask_sha256, token_limit, lens_text, lens_sha256, sources_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    lens_id,
                    decision["route"],
                    ask_sha,
                    decision["token_limit"],
                    lens,
                    lens_sha,
                    json.dumps({
                        "purpose": purpose,
                        "query_variants": decision["query_variants"],
                        "evidence_sufficiency": sufficiency,
                        "web_snippet_plan": web_plan,
                        "merged_winner_architecture": winner,
                        "tiny_prompt_engineer_card_50_words": tiny_card,
                        "fabric_layer": fabric_layer,
                        "axiomatic_retrieval_match_50_words": retrieval_match,
                        "qwen_chooser_lens_100_words": qwen_lens,
                        "rolling_recursive_triplet_card": rolling_card,
                        "tiny_model_status": tiny_status,
                        "nominal_context": nominal_context,
                        "behavior_pack": behavior_pack,
                        "markov_state": markov_state,
                        "logic_sources": sources,
                        "code_sources": code_sources,
                    }, ensure_ascii=True, sort_keys=True),
                ),
            )
            conn.execute(
                """
                INSERT INTO DATA_RETRIEVAL_EVENTS (
                    event_id, ask_sha256, ask_preview, route, token_limit, lens_id, result_count, decision_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    event_id,
                    ask_sha,
                    ask[:240],
                    decision["route"],
                    decision["token_limit"],
                    lens_id,
                    len(sources),
                    json.dumps(decision, ensure_ascii=True, sort_keys=True),
                ),
            )
            conn.execute(
                """
                INSERT INTO AI_CHOOSER_REVIEWS (
                    review_id, ask_sha256, lens_id, template_id, route,
                    draft_lens_sha256, reviewed_lens_sha256, review_status, review_json, reviewed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                (
                    unique_prefix("AICHOOSER", ask_sha),
                    ask_sha,
                    lens_id,
                    template_id,
                    decision["route"],
                    lens_sha,
                    sha256_text(qwen_lens.get("text", "")),
                    qwen_lens.get("status", "reviewed_by_qwen2_5"),
                    json.dumps({
                            "purpose": "real_qwen_rewrite_fabric_prompt",
                            "retrieval_epoch": "purpose_db_smollm_qwen_rolling_triplet",
                            "purpose_card_15": purpose["ask_card_15"],
                            "evidence_sufficiency": sufficiency,
                            "merged_winner_architecture": winner,
                            "tiny_prompt_engineer_card_50_words": tiny_card,
                            "fabric_layer": fabric_layer,
                            "axiomatic_retrieval_match": retrieval_match,
                            "qwen_lens": qwen_lens,
                            "rolling_card": rolling_card,
                            "rules": [
                                "preserve route and safety gates",
                                "reduce noise",
                                "prefer project-local successful logic",
                                "make topological instructions clearer",
                                "use purpose -> evidence cards -> web snippet plan -> task directions",
                            ],
                        }, ensure_ascii=True, sort_keys=True),
                ),
            )
            if decision["route"] in {"planning", "build"}:
                webcrawl_request_id = unique_prefix("WCRAWL", ask_sha)
                conn.execute(
                    """
                    INSERT INTO WEBCRAWL_RESEARCH_REQUESTS (
                        request_id, ask_sha256, route, query_json, noise_policy_json
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (
                        webcrawl_request_id,
                        ask_sha,
                        decision["route"],
                        json.dumps({
                            "tokens": decision["tokens"][:16],
                            "query_variants": decision["query_variants"][:6],
                            "ask_preview": ask[:240],
                            "purpose": purpose["purpose"],
                            "fabric_layer": fabric_layer,
                            "web_snippet_plan": web_plan,
                            "merged_winner_architecture": winner,
                            "tiny_prompt_engineer_card_50_words": tiny_card,
                            "qwen_chooser_lens_100_words": qwen_lens.get("text", ""),
                            "axiomatic_retrieval_match_50_words": retrieval_match.get("text", ""),
                        }, ensure_ascii=True, sort_keys=True),
                        json.dumps(policy, ensure_ascii=True, sort_keys=True),
                    ),
                )
                contract = {
                    "loop_count": 20 if decision["route"] == "build" else 3,
                    "actor_critic": "stop_best",
                    "mode": "proposal_only",
                    "karoo": "compare three options per advancement",
                    "abliterated": "crawl/suggest only",
                    "qwen_chooser": "real tiny chooser writes active lens",
                    "smollm_retrieval": "real tiny retrieval matcher injects closest 50 words",
                    "ministry": "fault-filter and toss failed triplet back",
                    "webcrawl_request_id": webcrawl_request_id,
                    "template_id": template_id,
                    "triplet_id": triplet_id,
                    "axiomatic_match_id": match_id,
                    "successful_code_sources": [item["sha256"] for item in code_sources[:5]],
                }
                conn.execute(
                    """
                    INSERT INTO KAROO_EPOCH_REQUESTS (
                        epoch_id, ask_sha256, route, loop_count, actor_critic, contract_json
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    (
                        epoch_id,
                        ask_sha,
                        decision["route"],
                        contract["loop_count"],
                        contract["actor_critic"],
                        json.dumps(contract, ensure_ascii=True, sort_keys=True),
                    ),
                )
            conn.commit()
    except sqlite3.Error:
        pass
    return {
        "event_id": event_id,
        "lens_id": lens_id,
        "ask_sha256": ask_sha,
        "route": decision["route"],
        "fabric_layer": fabric_layer,
        "token_limit": decision["token_limit"],
        "result_count": len(sources),
        "code_result_count": len(code_sources),
        "template_id": template_id,
        "axiomatic_match_id": match_id,
        "triplet_id": triplet_id,
        "purpose": purpose,
        "evidence_sufficiency": sufficiency,
        "web_snippet_plan": web_plan,
        "merged_winner_architecture": winner,
        "tiny_prompt_engineer_card_50_words": tiny_card,
        "nominal_context": nominal_context,
        "behavior_pack": behavior_pack,
        "markov_state": markov_state,
        "axiomatic_retrieval_match_50_words": retrieval_match,
        "qwen_chooser_lens_100_words": qwen_lens,
        "rolling_recursive_triplet_card": rolling_card,
        "tiny_model_status": tiny_status,
        "lens": lens,
        "lens_sha256": sha256_text(lens),
    }


def main():
    parser = argparse.ArgumentParser(description="VIPER data retrieval agent and Fabric lens crafter.")
    parser.add_argument("ask", nargs="*", help="Ask text. If omitted, stdin is used.")
    parser.add_argument("--json", action="store_true", help="Emit full JSON result.")
    args = parser.parse_args()
    ask = " ".join(args.ask).strip()
    if not ask:
        ask = input().strip()
    result = craft_lens(ask)
    if args.json:
        print(json.dumps(result, ensure_ascii=True, indent=2))
    else:
        print(result["lens"])


if __name__ == "__main__":
    main()
