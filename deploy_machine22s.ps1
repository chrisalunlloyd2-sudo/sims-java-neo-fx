$ErrorActionPreference = 'Stop'

Write-Host "====================================================="
Write-Host "   GODHAND ASCENSION - GITHUB DEPLOYMENT (MACHINE22S)"
Write-Host "====================================================="
Write-Host "Initiating fully autonomous push to private repo 'machine22s'."

# Check if GitHub CLI is installed. If not, auto-install via winget.
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Host "GitHub CLI not found. Auto-installing... Please wait."
    winget install --id GitHub.cli -e --accept-package-agreements --accept-source-agreements
    $env:Path += ";C:\Program Files\GitHub CLI\"
}

Write-Host "Authenticating with GitHub..."
# This will open a browser for a 1-click seamless login. No tokens to copy-paste.
gh auth login --web

Write-Host "Creating private repository 'machine22s'..."
gh repo create machine22s --private --source=. --remote=origin --push

Write-Host "====================================================="
Write-Host "DEPLOYMENT SUCCESSFUL. The organism is online."
Write-Host "====================================================="
Read-Host "Press Enter to close."
