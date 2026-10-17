@REM Kich ban kiem thu 2 nguoi choi thuc hien boi Phan Huu Tri - MSSV 2380602317
@REM Do dac goi tin UDP cong 9999 va kiem thu mang Tailscale
@echo off
chcp 65001 >nul
echo ========================================================
echo   KHOI DONG TEST GAME HELICOPTER MULTIPLAYER (2 NGUOI)
echo ========================================================

echo [1/3] Dang bien dich Server...
cd /d "%~dp0HelicopterServer"
if not exist "target\classes" mkdir target\classes
javac -encoding UTF-8 -d target/classes src/main/java/game/*.java src/main/java/network/*.java src/main/java/main/*.java
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Bien dich Server that bai!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Dang bien dich Client...
cd /d "%~dp0HelicopterClient"
if not exist "target\classes" mkdir target\classes
javac -encoding UTF-8 -d target/classes src/main/java/game/*.java src/main/java/gui/*.java src/main/java/network/*.java src/main/java/main/*.java
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Bien dich Client that bai!
    pause
    exit /b %ERRORLEVEL%
)

echo [3/3] Dang khoi dong Server va 2 Clients...
start "Helicopter Server" cmd /k "cd /d "%~dp0HelicopterServer" && title Helicopter Server && java -cp target/classes main.ServerMain"

timeout /t 2 /nobreak >nul

start "Helicopter Client 1" cmd /c "cd /d "%~dp0HelicopterClient" && java -cp target/classes main.ClientMain"
start "Helicopter Client 2" cmd /c "cd /d "%~dp0HelicopterClient" && java -cp target/classes main.ClientMain"

echo.
echo ========================================================
echo   DA KHOI DONG XONG!
echo   1. O Client 1: Nhap ten, bam Connect -> Ready
echo   2. O Client 2: Nhap ten, bam Connect -> Ready
echo   3. Server dem nguoc 5s va bat dau tran dau!
echo ========================================================
