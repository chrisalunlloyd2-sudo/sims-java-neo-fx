import sqlite3
c = sqlite3.connect('swarm_ledger.db')
tables = c.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
print("Tables:", tables)
try:
    rows = c.execute("SELECT * FROM EVENT_LOG ORDER BY id DESC LIMIT 5").fetchall()
    for r in rows:
        print(r)
except Exception as e:
    print("Error:", e)
c.close()
