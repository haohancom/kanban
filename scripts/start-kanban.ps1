param()

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")
$BackendDir = Join-Path $ProjectRoot "backend"
$LogDir = Join-Path $BackendDir "logs"
$PidFile = Join-Path $LogDir "kanban.pid"
$OutLog = Join-Path $LogDir "kanban.log"
$ErrLog = Join-Path $LogDir "kanban-error.log"

New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

if (Test-Path $PidFile) {
    $runningPid = (Get-Content $PidFile -ErrorAction SilentlyContinue).Trim()
    if ($runningPid) {
        $process = Get-Process -Id $runningPid -ErrorAction SilentlyContinue
        if ($process) {
            throw "检测到已有服务进程(PID=$runningPid)在运行，请先执行 stop-kanban.ps1"
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "启动前请确认已安装 Java 8+ 与 Maven"

Set-Location $BackendDir
mvn -f "pom.xml" -DskipTests clean package
if ($LASTEXITCODE -ne 0) {
    throw "maven 打包失败"
}

$jar = Get-ChildItem -Path (Join-Path $BackendDir "target") -Filter "kanban-*.jar" -File |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    throw "未找到打包后的 jar 文件"
}

$process = Start-Process -FilePath "java" -ArgumentList "-jar", $jar.FullName `
    -WorkingDirectory $BackendDir `
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
Write-Host "如要停止服务，请运行 stop-kanban.ps1"
