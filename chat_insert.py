import sqlite3
import sys
import os

db = os.path.join(os.path.dirname(os.path.abspath(__file__)), "swarm_ledger.db")
msg = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "empty"
msg = msg.replace("'", "").replace('"', '')

conn = sqlite3.connect(db)
conn.execute("INSERT INTO EVENT_LOG (sender, receiver, payload, status) VALUES ('WEB_USER', 'SWARM', ?, 'SUCCESS')", (msg,))
conn.commit()
conn.close()
