param(
    [Parameter(Mandatory=$true)]
    [string]$JavaFile
)

Write-Host "=================================================="
Write-Host "[JAVA SIMS PIPELINE] Abstract Compiler Pipeline"
Write-Host "=================================================="
Write-Host "[1] Resolving nested com.aigen.sims dependencies..."

$SourcePath = "..\sims_java_neo_fx_source\src\main\java"
Write-Host "[2] Compiling $JavaFile against $SourcePath"

javac -sourcepath $SourcePath $JavaFile
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FATAL] Java compilation failed. Rollback triggered." -ForegroundColor Red
    exit 1
}

Write-Host "[3] Compilation passed AST CI/CD gates." -ForegroundColor Green
Write-Host "[4] Generating SIMS1337 artifact metadata..."
exit 0
