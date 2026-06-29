param(
    [string]$JarPath = "backend/target/kanban-0.0.1-SNAPSHOT.jar",
    [string]$HostIp = "0.0.0.0"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
$ResolvedJarPath = Resolve-Path (Join-Path $ProjectRoot $JarPath) -ErrorAction SilentlyContinue

if (-not $ResolvedJarPath) {
    throw "未找到 jar 文件: $JarPath"
}

$Jar = Get-Item -Path $ResolvedJarPath
$JarDir = $Jar.DirectoryName
$LogDir = Join-Path $JarDir "logs"
$PidFile = Join-Path $LogDir "kanban-runtime.pid"
$OutLog = Join-Path $LogDir "kanban-runtime.log"
$ErrLog = Join-Path $LogDir "kanban-runtime-error.log"

New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

if (Test-Path $PidFile) {
    $runningPid = (Get-Content $PidFile -ErrorAction SilentlyContinue).Trim()
    if ($runningPid) {
        $process = Get-Process -Id $runningPid -ErrorAction SilentlyContinue
        if ($process) {
            throw "检测到已有服务进程(PID=$runningPid)在运行，请先执行 stop-kanban-jar.ps1"
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
}

$arguments = @("-Dserver.address=$HostIp", "-jar", $Jar.FullName)

$process = Start-Process -FilePath "java" -ArgumentList $arguments `
    -WorkingDirectory $JarDir `
    -RedirectStandardOutput $OutLog `
    -RedirectStandardError $ErrLog `
    -PassThru `
    -WindowStyle Hidden

if (-not $process -or -not $process.Id) {
    throw "启动进程失败"
}

$process.Id | Out-File -FilePath $PidFile -Encoding ASCII -NoNewline
Write-Host "启动成功，PID=$($process.Id)"
Write-Host "访问地址: http://localhost:8080"
Write-Host "日志: $OutLog"
Write-Host "如要停止服务，请运行 stop-kanban-jar.ps1"
