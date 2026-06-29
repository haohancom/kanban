@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-kanban.ps1"
exit /b %errorlevel%
