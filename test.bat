@echo off
echo ========================================
echo   Running Banking System Test Suite...
echo ========================================

if not exist out\banking\BankingSystemTest.class (
    echo Building project and test suite first...
    call build.bat
    if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
)

java -cp out banking.BankingSystemTest
