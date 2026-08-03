# Master script to ensure ALL processes are fully autonomous and running

Write-Host "TERMINATING ALL ZOMBIE PROCESSES..."
Stop-Process -Name "python" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue

$KAROO_DIR = "C:\Users\viper\OneDrive\Desktop\kstaats-karoo_gp-1bc3859"
$JAVA_DIR = "C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx"

Write-Host "BOOTING SYSTEM 1: Dashboard Server..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-WindowStyle Hidden -Command cd '$KAROO_DIR'; python sims1337_dashboard_server.py" -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 2: Python Swarm (Autonomous Core, Scaler, Watchdog)..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-WindowStyle Hidden -Command cd '$KAROO_DIR'; python launch_swarm.py" -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 3: AEGIS GodHand Java GUI (Real Inference)..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\launch_gui.ps1" -WorkingDirectory $JAVA_DIR -WindowStyle Normal

Write-Host "ALL SYSTEMS ONLINE. ENGAGING."
