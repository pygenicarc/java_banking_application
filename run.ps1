Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Launching Java Banking System..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Test-Path "out\banking\Main.class")) {
    Write-Host "Classes not found. Compiling first..." -ForegroundColor Yellow
    & ".\build.ps1"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

java -cp out banking.Main
