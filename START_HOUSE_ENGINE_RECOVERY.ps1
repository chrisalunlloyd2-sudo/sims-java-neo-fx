Write-Host '======================================'
Write-Host '[GODHAND SWARM OVERRIDE: START_HOUSE_ENGINE_RECOVERY.ps1]'
Write-Host '======================================'
Write-Host 'Hooking into ZMQ Bus...'
python -c "import sqlite3; c=sqlite3.connect('swarm_ledger.db'); c.execute('INSERT INTO EVENT_LOG (timestamp, sender, receiver, payload, status) VALUES (CURRENT_TIMESTAMP, \'GUI_MANIFOLD\', \'SWARM\', \'TRIGGERED START_HOUSE_ENGINE_RECOVERY.ps1\', \'ACTIVE\')'); c.commit()"
Write-Host 'Injection Complete. Waiting for agent assimilation...'
Start-Sleep -Seconds 2
exit 0
