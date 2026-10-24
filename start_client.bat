@echo off
chcp 65001 >nul
cd /d "%~dp0HelicopterClient"

set "JAVAC_CMD=javac"
set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-24\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Java\jdk-24\bin\javac.exe"
    set "JAVA_CMD=C:\Program Files\Java\jdk-24\bin\java.exe"
)

if not exist "target\classes" mkdir target\classes
echo Dang bien dich toan bo ma nguon Client...
dir /s /b src\main\java\*.java > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -d target/classes @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the bien dich Client.
    if exist sources.txt del sources.txt
    pause
    exit /b %ERRORLEVEL%
)
if exist sources.txt del sources.txt

echo Dang khoi chay Client...
"%JAVA_CMD%" -cp target/classes main.ClientMain
pause
