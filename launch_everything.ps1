# Master script to ensure ALL processes across both repos are fully autonomous and running

Write-Host "TERMINATING ALL ZOMBIE PROCESSES..."
Stop-Process -Name "python" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue

$KAROO_DIR = "C:\Users\viper\OneDrive\Desktop\kstaats-karoo_gp-1bc3859"
$JAVA_DIR = "C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx"
$DESKTOP_MAIN_DIR = "C:\Users\viper\OneDrive\Desktop\local_desktop-main"

Write-Host "BOOTING SYSTEM 1: Dashboard Server (Karoo)..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-WindowStyle Hidden -Command cd '$KAROO_DIR'; python sims1337_dashboard_server.py" -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 2: Python Swarm (Autonomous Core, Scaler, Watchdog)..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-WindowStyle Hidden -Command cd '$KAROO_DIR'; python launch_swarm.py" -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 3: Logic Blockchain Shipper..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\START_LOGIC_BLOCKCHAIN_PORT.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 4: Topology Sidecar..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\START_TOPOLOGY_SIDECAR.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 5: House Inference Engine..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\START_HOUSE_ENGINE_RECOVERY.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 6: Lab Suite Server..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\START_LAB_SUITE.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 7: Notes Suite Server..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\START_NOTES_SUITE.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 8: Notes Cloudflare Tunnel..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\START_NOTES_TUNNEL.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 9: Moltbook Triplet Loop..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\LAUNCH_MOLTBOOK.ps1" -WorkingDirectory $DESKTOP_MAIN_DIR -WindowStyle Hidden

Write-Host "BOOTING SYSTEM 10: AEGIS GodHand Java GUI (Real Inference)..."
Start-Process -FilePath "powershell.exe" -ArgumentList "-ExecutionPolicy Bypass -File .\launch_gui.ps1" -WorkingDirectory $JAVA_DIR -WindowStyle Normal

Write-Host "ALL SYSTEMS ONLINE. ENGAGING THE MANIFOLD."
