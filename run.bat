@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Helper script to set up and run the crime aggregation API on Windows.
REM Usage:
REM   run.bat             -> install deps (if needed) and start the API
REM   run.bat --ingest    -> also run the Police API ingestion (3 months)
REM   run.bat --help      -> show this message

for %%I in (%*) do (
    if /I "%%I"=="--help" goto :show_help
)

set ROOT_DIR=%~dp0
set SERVICE_DIR=%ROOT_DIR%services\crime-aggregator
set VENV_DIR=%SERVICE_DIR%\.venv

if not exist "%SERVICE_DIR%" (
    echo [ERROR] services\crime-aggregator was not found. Please run this script from the repo root.
    goto :eof
)

if not exist "%VENV_DIR%\Scripts\python.exe" (
    echo [INFO] Creating Python virtual environment...
    python -m venv "%VENV_DIR%"
    if errorlevel 1 (
        echo [ERROR] Failed to create the virtual environment. Ensure Python 3.11+ is installed and on PATH.
        goto :eof
    )
)

echo [INFO] Activating virtual environment...
call "%VENV_DIR%\Scripts\activate.bat"
if errorlevel 1 (
    echo [ERROR] Unable to activate the virtual environment.
    goto :eof
)

echo [INFO] Installing/Updating dependencies...
pip install --upgrade pip >nul
pip install -e "%SERVICE_DIR%"
if errorlevel 1 (
    echo [ERROR] Failed to install Python dependencies.
    goto :cleanup
)

set DO_INGEST=0
for %%I in (%*) do (
    if /I "%%I"=="--ingest" set DO_INGEST=1
)

if "%DO_INGEST%"=="1" (
    echo [INFO] Running Police API ingestion for the last 3 months...
    crime-aggregator-ingest --months 3
    if errorlevel 1 (
        echo [WARN] Ingestion failed. Check your network connection or API availability.
    )
)

echo [INFO] Starting the crime aggregation API on http://0.0.0.0:8000/ ...
crime-aggregator-api

:cleanup
call "%VENV_DIR%\Scripts\deactivate.bat" >nul 2>nul
endlocal
exit /b %ERRORLEVEL%

:show_help
echo SafeRouting helper script

echo   run.bat             Install dependencies (if needed) and launch the API server

echo   run.bat --ingest    Run Police API ingestion before launching the server

echo   run.bat --help      Display this help
exit /b 0
