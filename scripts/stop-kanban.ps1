$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
$BackendDir = Join-Path $ProjectRoot "backend"
$LogDir = Join-Path $BackendDir "logs"
$PidFile = Join-Path $LogDir "kanban.pid"

if (Test-Path $PidFile) {
    $pidText = (Get-Content $PidFile -ErrorAction SilentlyContinue).Trim()
    if ($pidText) {
        $process = Get-Process -Id $pidText -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $process.Id -Force
            Write-Host "已停止进程 PID=$($process.Id)"
        }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

$pattern = Join-Path $BackendDir "target\kanban-*.jar"
$jarMatches = Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*.original" }
$jarNames = @($jarMatches | ForEach-Object Name)

if ($jarNames.Count -gt 0) {
    $running = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object {
            foreach ($jarName in $jarNames) {
                if ($_.CommandLine -like "*$jarName*") {
                    return $true
                }
            }
            return $false
        }

    foreach ($proc in $running) {
        Stop-Process -Id $proc.ProcessId -Force
        Write-Host "已停止匹配进程 PID=$($proc.ProcessId)"
    }
}

Write-Host "停止完成"
