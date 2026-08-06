$ErrorActionPreference = "Stop"

Write-Host "Configuring Zero-Touch Automation for SIMS1337 Grid..." -ForegroundColor Cyan

# 1. Hive Daemon Auto-Boot (Runs silently on User Login)
$daemonAction = New-ScheduledTaskAction -Execute "PowerShell.exe" -Argument "-WindowStyle Hidden -ExecutionPolicy Bypass -File C:\Users\viper\OneDrive\Desktop\local_desktop-main\START_HIVE_DAEMON.ps1"
$daemonTrigger = New-ScheduledTaskTrigger -AtLogon
Register-ScheduledTask -Action $daemonAction -Trigger $daemonTrigger -TaskName "SIMS1337_Hive_Daemon" -Description "Auto-starts the Neuromorphic Grid on user login" -Force

# 2. Night Cycle Dream Phase (Runs daily at Midnight 00:00)
$nightAction = New-ScheduledTaskAction -Execute "C:\Users\viper\AppData\Local\Programs\Python\Python311\python.exe" -Argument "C:\Users\viper\OneDrive\Desktop\local_desktop-main\night_cycle_dream.py" -WorkingDirectory "C:\Users\viper\OneDrive\Desktop\local_desktop-main"
$nightTrigger = New-ScheduledTaskTrigger -Daily -At "00:00"
Register-ScheduledTask -Action $nightAction -Trigger $nightTrigger -TaskName "SIMS1337_Night_Cycle" -Description "Triggers the Dream Phase every midnight" -Force

Write-Host "Automation successfully implanted into Windows Task Scheduler." -ForegroundColor Green
