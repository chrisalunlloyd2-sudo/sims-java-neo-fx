Write-Host "INITIALIZING NEUROMORPHIC HIVE DAEMON..." -ForegroundColor Cyan

$env:GIST_TOKEN = $env:GIST_TOKEN
$pythonPath = "C:\Users\viper\AppData\Local\Programs\Python\Python311\python.exe"

# Base Scripts
$scripts = @(
    "global_agent_network.py",
    "risc_bridge_server.py",
    "topology_sidecar.py",
    "loihi_spike_sidecar.py",
    "house_inference_engine.py",
    "performative_router.py",
    "data_retrieval_lens_agent.py",
    "missed_message_relay.py",
    "security_sentinel.py"
)

foreach ($script in $scripts) {
    if (Test-Path $script) {
        Write-Host "SPINNING UP NODE: $script" -ForegroundColor Green
        Start-Process -FilePath $pythonPath -ArgumentList $script -WindowStyle Minimized
    } else {
        Write-Host "WARNING: Node script not found - $script" -ForegroundColor Yellow
    }
}

Write-Host "HIVE DAEMON ARMED AND ACTIVE." -ForegroundColor Cyan
Write-Host "Syncing with Cloudflare atomic clock and Gist FOW pipeline..." -ForegroundColor DarkGray
