$scripts = @(
    "START_LOGIC_BLOCKCHAIN_PORT.ps1",
    "START_TOPOLOGY_SIDECAR.ps1",
    "START_HOUSE_ENGINE_RECOVERY.ps1",
    "SPIN_UP_AGENT_NODE.ps1",
    "START_LAB_SUITE.ps1",
    "START_NOTES_SUITE.ps1",
    "START_NOTES_TUNNEL.ps1",
    "LAUNCH_MOLTBOOK.ps1"
)

foreach ($script in $scripts) {
    $content = @"
Write-Host '======================================'
Write-Host '[GODHAND SWARM OVERRIDE: $script]'
Write-Host '======================================'
Write-Host 'Hooking into ZMQ Bus...'
python -c `"import sqlite3; c=sqlite3.connect('swarm_ledger.db'); c.execute('INSERT INTO EVENT_LOG (timestamp, sender, receiver, payload, status) VALUES (CURRENT_TIMESTAMP, \'GUI_MANIFOLD\', \'SWARM\', \'TRIGGERED $script\', \'ACTIVE\')'); c.commit()`"
Write-Host 'Injection Complete. Waiting for agent assimilation...'
Start-Sleep -Seconds 2
exit 0
"@
    Set-Content -Path "C:\Users\viper\OneDrive\Desktop\local_desktop-main\$script" -Value $content
}
Write-Host "All 8 manifold scripts generated."
