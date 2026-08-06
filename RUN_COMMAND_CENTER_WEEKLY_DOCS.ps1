$ErrorActionPreference = "Stop"
$root = "C:\Users\viper\VIPER_JAVA_RISC"
$config = Join-Path $root "ops\command_center_maintenance\maintenance_policy.json"
$logDir = Join-Path $root "logs\command_center_maintenance"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$stamp = Get-Date -Format "yyyyMMddTHHmmss"
$log = Join-Path $logDir "weekly_docs_$stamp.log"
& py -3 (Join-Path $root "tools\command_center_maintenance.py") --config $config weekly-docs *>> $log
