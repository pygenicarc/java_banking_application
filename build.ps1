Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Compiling Java Banking System..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Test-Path "out")) {
    New-Item -ItemType Directory -Path "out" | Out-Null
}

$sources = Get-ChildItem -Recurse -Filter *.java -Path src, test | ForEach-Object { $_.FullName }
javac -d out $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Compilation completed successfully!" -ForegroundColor Green
    Write-Host "Class files are generated in the 'out' directory."
} else {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}
