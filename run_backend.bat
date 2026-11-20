@echo off
title Chama Loan AI Underwriting API (FastAPI)
echo ========================================================
echo   Starting Chama AI Underwriting & Risk Engine Backend
echo   Target: http://0.0.0.0:8000 (Accessible via 10.0.2.2 in Emulator)
echo ========================================================
cd /d "%~dp0backend"
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
pause
