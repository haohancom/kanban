$ErrorActionPreference = "Stop"

param(
    [string]$JarPath = "backend/target/kanban-0.0.1-SNAPSHOT.jar"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
$ResolvedJarPath = Resolve-Path (Join-Path $ProjectRoot $JarPath) -ErrorAction SilentlyContinue

if (-not $ResolvedJarPath) {
    Write-Host "未找到 jar 文件: $JarPath"
} else {
    $Jar = Get-Item -Path $ResolvedJarPath
    $JarDir = $Jar.DirectoryName
    $LogDir = Join-Path $JarDir "logs"
    $PidFile = Join-Path $LogDir "kanban-runtime.pid"

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

    $running = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like "*$($Jar.Name)*" }

    foreach ($proc in $running) {
        Stop-Process -Id $proc.ProcessId -Force
        Write-Host "已停止匹配进程 PID=$($proc.ProcessId)"
    }
}

Write-Host "停止完成"
