@echo off
echo ========================================
echo   Compiling Java Banking System...
echo ========================================

if not exist out mkdir out

javac -d out src\banking\*.java src\banking\model\*.java src\banking\service\*.java src\banking\util\*.java src\banking\exception\*.java test\banking\*.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully!
    echo Class files are in the 'out' directory.
) else (
    echo [ERROR] Compilation failed. Please check errors above.
    exit /b %ERRORLEVEL%
)
