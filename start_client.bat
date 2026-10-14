@echo off
chcp 65001 >nul
cd /d "%~dp0HelicopterClient"
if not exist "target\classes" mkdir target\classes
echo Dang bien dich Client...
javac -encoding UTF-8 -d target/classes src/main/java/game/*.java src/main/java/gui/*.java src/main/java/network/*.java src/main/java/main/*.java
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the bien dich Client.
    pause
    exit /b %ERRORLEVEL%
)
echo Dang khoi chay Client...
java -cp target/classes main.ClientMain
pause
