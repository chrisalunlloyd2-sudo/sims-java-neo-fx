# SIMS1337 - Master Launcher
# Starts all daemons: GUI, Bridge, Telemetry, Cellular Microphone
# Run this once and everything stays alive

$PYTHON = "C:\Users\viper\AppData\Local\Programs\Python\Python311\python.exe"
$ROOT = "C:\Users\viper\OneDrive\Desktop\local_desktop-main"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  SIMS1337 NEUROMORPHIC GRID - MASTER LAUNCH" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Kill stale processes
Write-Host "[1/6] Cleaning stale processes..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep 1

# Step 2: Initialize DB if needed
Write-Host "[2/6] Ensuring database..." -ForegroundColor Yellow
if (-not (Test-Path "$ROOT\swarm_ledger.db")) {
    & $PYTHON "$ROOT\init_db.py"
    Write-Host "  Database initialized" -ForegroundColor Green
} else {
    Write-Host "  Database exists" -ForegroundColor Green
}

# Step 3: Start Telemetry Collector
Write-Host "[3/6] Starting Telemetry Collector..." -ForegroundColor Yellow
Start-Process -FilePath $PYTHON -ArgumentList "$ROOT\telemetry_collector.py" -WorkingDirectory $ROOT -WindowStyle Hidden
Write-Host "  Telemetry daemon started" -ForegroundColor Green

# Step 4: Start Bridge
Write-Host "[4/6] Starting GUI State Bridge..." -ForegroundColor Yellow
Start-Process -FilePath $PYTHON -ArgumentList "$ROOT\gui_state_bridge.py" -WorkingDirectory $ROOT -WindowStyle Hidden
Write-Host "  Bridge daemon started" -ForegroundColor Green

# Step 5: Start Java GUI + Web Server
Write-Host "[5/6] Launching Java GUI + Web UI..." -ForegroundColor Yellow
Push-Location "$ROOT\sims_java_neo_fx_source"
Start-Process powershell -ArgumentList "-ExecutionPolicy Bypass -File .\LAUNCH_REAL_GUI.ps1" -WindowStyle Minimized
Pop-Location
Write-Host "  GUI launching on port 1337" -ForegroundColor Green

Start-Sleep 3

# Step 6: Start Cellular Microphone (optional - run separately for control)
Write-Host "[6/6] Cellular Microphone ready..." -ForegroundColor Yellow
Write-Host "  To start: & $PYTHON $ROOT\cellular_microphone.py" -ForegroundColor White
Write-Host ""

Write-Host "============================================" -ForegroundColor Green
Write-Host "  ALL SYSTEMS ONLINE" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Local GUI:  Running (JavaFX)" -ForegroundColor Cyan
Write-Host "  Web UI:     http://localhost:1337" -ForegroundColor Cyan
Write-Host "  Phone:      http://192.168.0.180:1337" -ForegroundColor Cyan
Write-Host "  Bridge:     Polling every 1s" -ForegroundColor Cyan
Write-Host "  Telemetry:  CPU/RAM/GPU every 5s" -ForegroundColor Cyan
Write-Host "  Queen Bee:  dagbs/qwen2.5-coder-3b-abliterated:q8_0" -ForegroundColor Magenta
Write-Host ""
Write-Host "  Nothing runs for free." -ForegroundColor DarkGray
Write-Host "  Nothing lives forever." -ForegroundColor DarkGray
Write-Host "  Always advancing. Always learning. Always progressing." -ForegroundColor DarkGray
Write-Host ""
