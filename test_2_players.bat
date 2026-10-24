@REM Kich ban kiem thu 2 nguoi choi thuc hien boi Phan Huu Tri - MSSV 2380602317
@REM Do dac goi tin UDP cong 9999 va kiem thu mang Tailscale
@echo off
chcp 65001 >nul
echo ========================================================
echo   KHOI DONG TEST GAME HELICOPTER MULTIPLAYER (2 NGUOI)
echo ========================================================

set "JAVAC_CMD=javac"
set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-24\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Java\jdk-24\bin\javac.exe"
    set "JAVA_CMD=C:\Program Files\Java\jdk-24\bin\java.exe"
)

echo [1/3] Dang bien dich Server...
cd /d "%~dp0HelicopterServer"
if not exist "target\classes" mkdir target\classes
dir /s /b src\main\java\*.java > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -d target/classes @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Bien dich Server that bai!
    if exist sources.txt del sources.txt
    pause
    exit /b %ERRORLEVEL%
)
if exist sources.txt del sources.txt

echo [2/3] Dang bien dich Client...
cd /d "%~dp0HelicopterClient"
if not exist "target\classes" mkdir target\classes
dir /s /b src\main\java\*.java > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -d target/classes @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Bien dich Client that bai!
    if exist sources.txt del sources.txt
    pause
    exit /b %ERRORLEVEL%
)
if exist sources.txt del sources.txt

echo [3/3] Dang khoi dong Server va 2 Clients...
start "Helicopter Server" cmd /k "cd /d "%~dp0HelicopterServer" && title Helicopter Server && "%JAVA_CMD%" -cp target/classes main.ServerMain"

timeout /t 2 /nobreak >nul

start "Helicopter Client 1" cmd /c "cd /d "%~dp0HelicopterClient" && "%JAVA_CMD%" -cp target/classes main.ClientMain"
start "Helicopter Client 2" cmd /c "cd /d "%~dp0HelicopterClient" && "%JAVA_CMD%" -cp target/classes main.ClientMain"

echo.
echo ========================================================
echo   DA KHOI DONG XONG!
echo   1. O Client 1: Nhap ten, bam Connect -> Ready
echo   2. O Client 2: Nhap ten, bam Connect -> Ready
echo   3. Server dem nguoc 5s va bat dau tran dau!
echo ========================================================
