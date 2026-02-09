@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "PROPS=%ROOT%deskpilot.properties"

echo [deskpilotw] ROOT=%ROOT%
echo [deskpilotw] PROPS=%PROPS%

if not exist "%PROPS%" (
  echo [deskpilotw][ERROR] deskpilot.properties not found: %PROPS%
  exit /b 2
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROPS%") do (
  if /i "%%A"=="deskpilot.version" set "DP_VER=%%B"
)

if "%DP_VER%"=="" (
  echo [deskpilotw][ERROR] deskpilot.version missing in deskpilot.properties
  exit /b 2
)

set "CACHE=%ROOT%.deskpilot\cli\%DP_VER%"
set "JAR=%CACHE%\deskpilot.jar"

echo [deskpilotw] DP_VER=%DP_VER%
echo [deskpilotw] JAR=%JAR%

if not exist "%CACHE%" mkdir "%CACHE%" >nul 2>&1

if not exist "%JAR%" (
  echo [deskpilotw] CLI jar missing. Downloading...

  set "TAG=v%DP_VER%"
  set "URL=https://github.com/Sankr20/deskpilot/releases/download/%TAG%/deskpilot.jar"

  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { iwr -UseBasicParsing '%URL%' -OutFile '%JAR%' } catch { exit 1 }"

  if not exist "%JAR%" (
    echo [deskpilotw][ERROR] Download failed: %URL%
    exit /b 2
  )
)

echo [deskpilotw] running: java -jar "%JAR%" %*
java -jar "%JAR%" %*
set "RC=%ERRORLEVEL%"
echo [deskpilotw] exit code=%RC%
exit /b %RC%
