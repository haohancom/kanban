@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-kanban-jar.ps1" %*
exit /b %errorlevel%
