import os
import sqlite3
from pathlib import Path
import re

DB_PATH = "gemini_bridge.db"

def crawl_logic_sources():
    """
    [ERBCRAWL PATTERN] Extracts logic and sources from the codebase for the 8-hour look database.
    """
    print("🕸️ IGNITING LOGIC CRAWLER: Sourcing 101% Knowledge...")
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS LOGIC_SOURCES (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_file TEXT,
            logic_pattern TEXT,
            context TEXT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    """)

    target_dirs = [Path("VIPER_JAVA_RISC"), Path("Aegis_Agents"), Path(".old")]
    
    for target in target_dirs:
        if not target.exists(): continue
        for file_path in target.glob("**/*.py"):
            try:
                content = file_path.read_text(encoding="utf-8", errors="ignore")
                # Extract functions, class definitions, and logic-heavy blocks
                patterns = re.findall(r"(def \w+\(.*?\)[:\n]|class \w+[:\n]|if .*?:|while .*?:)", content)
                for pattern in patterns:
                    cursor.execute("INSERT INTO LOGIC_SOURCES (source_file, logic_pattern, context) VALUES (?, ?, ?)",
                                   (str(file_path), pattern.strip(), "Automated logic extraction."))
            except: pass
            
    conn.commit()
    cursor.execute("SELECT COUNT(*) FROM LOGIC_SOURCES")
    count = cursor.fetchone()[0]
    print(f"✅ CRAWL COMPLETE: {count} logic patterns sourced into RAG manifold.")
    conn.close()

if __name__ == "__main__":
    crawl_logic_sources()
