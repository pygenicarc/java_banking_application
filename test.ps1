Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Running Banking System Test Suite..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Test-Path "out\banking\BankingSystemTest.class")) {
    Write-Host "Classes not found. Compiling first..." -ForegroundColor Yellow
    & ".\build.ps1"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

java -cp out banking.BankingSystemTest
