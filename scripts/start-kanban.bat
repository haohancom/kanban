@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-kanban.ps1"
exit /b %errorlevel%
