@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-kanban-jar.ps1" %*
exit /b %errorlevel%
