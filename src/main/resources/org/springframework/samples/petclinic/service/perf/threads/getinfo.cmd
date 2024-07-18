@echo off
echo "Checking owner with phone=%1"
rem Emulating some heavy work...
ping /n 2 /w 1000 localhost >nul
exit /b 0
