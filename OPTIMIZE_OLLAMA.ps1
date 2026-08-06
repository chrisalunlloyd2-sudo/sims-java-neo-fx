# SIMS1337 - Ollama Environment Optimizer
# Sets optimal environment variables for mmap/SSD operation
# Run this BEFORE starting the cellular microphone

# === CORE OPTIMIZATIONS ===
# Flash attention: reduces memory usage significantly
$env:OLLAMA_FLASH_ATTENTION = "1"

# KV cache compression: q8_0 uses ~50% less memory than f16
$env:OLLAMA_KV_CACHE_TYPE = "q8_0"

# Max 2 models loaded: Queen Bee + 1 worker at a time
$env:OLLAMA_MAX_LOADED_MODELS = "2"

# 1 parallel request per model (workers are sequential)
$env:OLLAMA_NUM_PARALLEL = "1"

# Keep models directory on SSD for fast mmap
# Models are at: C:\Users\viper\.ollama\models
# mmap is ON by default - models are memory-mapped from SSD
# Only accessed pages are loaded into RAM (demand paging)

Write-Host "=== OLLAMA OPTIMIZATION APPLIED ===" -ForegroundColor Cyan
Write-Host "OLLAMA_FLASH_ATTENTION = $env:OLLAMA_FLASH_ATTENTION" -ForegroundColor Green
Write-Host "OLLAMA_KV_CACHE_TYPE   = $env:OLLAMA_KV_CACHE_TYPE" -ForegroundColor Green
Write-Host "OLLAMA_MAX_LOADED_MODELS = $env:OLLAMA_MAX_LOADED_MODELS" -ForegroundColor Green
Write-Host "OLLAMA_NUM_PARALLEL    = $env:OLLAMA_NUM_PARALLEL" -ForegroundColor Green
Write-Host ""
Write-Host "mmap Strategy:" -ForegroundColor Yellow
Write-Host "  - Models stay on SSD as files" -ForegroundColor White
Write-Host "  - OS pages them into RAM on demand" -ForegroundColor White
Write-Host "  - Workers use num_gpu=0 (CPU-only via mmap)" -ForegroundColor White
Write-Host "  - Workers use keep_alive=0 (evict after response)" -ForegroundColor White
Write-Host "  - Queen Bee uses num_gpu=99 (full GPU, never evicts)" -ForegroundColor White
Write-Host ""
Write-Host "Queen Bee: dagbs/qwen2.5-coder-3b-instruct-abliterated:q8_0" -ForegroundColor Magenta
Write-Host ""

# Restart Ollama with new settings
Write-Host "Restarting Ollama service..." -ForegroundColor Yellow
Stop-Process -Name "ollama" -Force -ErrorAction SilentlyContinue
Start-Sleep 2

# Start Ollama with optimized settings
$ollamaPath = (Get-Command ollama -ErrorAction SilentlyContinue).Source
if (-not $ollamaPath) {
    $ollamaPath = "$env:LOCALAPPDATA\Programs\Ollama\ollama.exe"
}

if (Test-Path $ollamaPath) {
    Start-Process -FilePath $ollamaPath -ArgumentList "serve" -WindowStyle Hidden
    Write-Host "Ollama restarted with optimizations!" -ForegroundColor Green
} else {
    Write-Host "Ollama path not found at $ollamaPath - it may already be running as a service" -ForegroundColor Yellow
}

Start-Sleep 3

# Verify Ollama is responding
try {
    $response = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5
    Write-Host "Ollama online: $($response.models.Count) models available" -ForegroundColor Green
} catch {
    Write-Host "Waiting for Ollama..." -ForegroundColor Yellow
    Start-Sleep 5
}

# Pre-warm Queen Bee (GPU-resident, never evict)
Write-Host ""
Write-Host "Pre-warming Queen Bee on GPU..." -ForegroundColor Magenta
$body = @{
    model = "dagbs/qwen2.5-coder-3b-instruct-abliterated:q8_0"
    prompt = "Say HIVE ONLINE"
    stream = $false
    options = @{ num_gpu = 99; num_ctx = 4096 }
    keep_alive = "-1"
} | ConvertTo-Json

try {
    $resp = Invoke-RestMethod -Uri "http://localhost:11434/api/generate" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 60
    Write-Host "Queen Bee ONLINE: $($resp.response)" -ForegroundColor Green
    Write-Host "Queen is GPU-resident and will NEVER be evicted" -ForegroundColor Green
} catch {
    Write-Host "Queen warm-up failed: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== READY FOR CELLULAR MICROPHONE ===" -ForegroundColor Cyan
Write-Host "Run: python cellular_microphone.py" -ForegroundColor White
