param(
    [string]$CommitMessage = "",
    [switch]$SkipGit,
    [switch]$SkipBackend,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$RepoRoot = $PSScriptRoot
$OracleHost = "129.159.115.194"
$OracleUser = "opc"
$SshKeyPath = Join-Path $RepoRoot ".ssh\oracle-amz.key\ssh-key-2026-04-01.key"
$BackendHealthUrl = "http://129.159.115.194/api/health"
$FrontendUrl = "https://amz-express.vercel.app"

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Run-Local {
    param([string]$Command)
    if ($DryRun) {
        Write-Host "[DRY-RUN][LOCAL] $Command" -ForegroundColor Yellow
        return
    }

    Write-Host "[LOCAL] $Command" -ForegroundColor Gray
    Invoke-Expression $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo ejecutando comando local: $Command"
    }
}

function Run-Remote {
    param([string]$RemoteCommand)
    if ($DryRun) {
        Write-Host "[DRY-RUN][REMOTE] $RemoteCommand" -ForegroundColor Yellow
        return
    }

    if (-not (Test-Path $SshKeyPath)) {
        throw "No se encontró la llave SSH en: $SshKeyPath"
    }

    & ssh -i $SshKeyPath -o StrictHostKeyChecking=no "$OracleUser@$OracleHost" $RemoteCommand
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo ejecutando despliegue remoto en Oracle."
    }
}

if (-not (Test-Path (Join-Path $RepoRoot ".git"))) {
    throw "Este script debe ejecutarse desde el root del repo (no se encontró .git)."
}

if (-not $SkipGit) {
    Write-Step "Sincronizando cambios con GitHub"

    $statusLines = git status --porcelain
    if ($statusLines) {
        Run-Local "git add -A"

        if (Test-Path (Join-Path $RepoRoot ".windsurf")) {
            Run-Local "git reset -- .windsurf"
        }
        if (Test-Path (Join-Path $RepoRoot ".ssh")) {
            Run-Local "git reset -- .ssh"
        }

        $staged = git diff --cached --name-only
        if ($staged) {
            if ([string]::IsNullOrWhiteSpace($CommitMessage)) {
                $CommitMessage = "chore: deploy latest amz express updates"
            }
            Run-Local "git commit -m `"$CommitMessage`""
        } else {
            Write-Host "No hay archivos staged para commit." -ForegroundColor DarkYellow
        }
    } else {
        Write-Host "No hay cambios locales para commit." -ForegroundColor DarkYellow
    }

    Run-Local "git push origin main"
}

$currentSha = (git rev-parse --short HEAD).Trim()
Write-Host "`nSHA actual: $currentSha" -ForegroundColor Green

if (-not $SkipBackend) {
    Write-Step "Desplegando backend en Oracle"

    $remoteDeployCommand = @"
set -e
cd /home/opc/apps/amz_express
git fetch origin main
git reset --hard origin/main
cd backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.18.0.8-1.0.1.el8.x86_64
export PATH=`$JAVA_HOME/bin:`$PATH
mvn -DskipTests package
sudo systemctl restart amz-express
for i in {1..45}; do
  if curl -fsS http://localhost:8080/api/health > /tmp/amz-health.json 2>/dev/null; then
    cat /tmp/amz-health.json
    exit 0
  fi
  sleep 2
done
sudo systemctl status amz-express --no-pager -n 60
exit 1
"@

    Run-Remote $remoteDeployCommand
}

Write-Step "Resumen"
Write-Host "Frontend (Vercel): $FrontendUrl" -ForegroundColor Green
Write-Host "Backend (Oracle health): $BackendHealthUrl" -ForegroundColor Green
Write-Host "Commit desplegado: $currentSha" -ForegroundColor Green
Write-Host "`nDeploy 1-click terminado." -ForegroundColor Cyan
