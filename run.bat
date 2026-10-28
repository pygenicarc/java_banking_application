@echo off
echo ========================================
echo   Launching Java Banking System...
echo ========================================

if not exist out\banking\Main.class (
    echo Compiled classes not found. Building first...
    call build.bat
    if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
)

java -cp out banking.Main
