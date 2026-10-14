@echo off
chcp 65001 >nul
cd /d "%~dp0HelicopterServer"
if not exist "target\classes" mkdir target\classes
echo Dang bien dich Server...
javac -encoding UTF-8 -d target/classes src/main/java/game/*.java src/main/java/network/*.java src/main/java/main/*.java
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the bien dich Server.
    pause
    exit /b %ERRORLEVEL%
)
echo Dang khoi chay Server...
java -cp target/classes main.ServerMain
pause
