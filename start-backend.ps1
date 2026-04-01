$ErrorActionPreference = 'SilentlyContinue'
$backendDir = "C:\Users\XaviXPC\Documents\Projects\amz-express-spring\backend"
$healthUrl = "http://localhost:8080/api/health"

function Test-BackendHealth {
    try {
        $resp = Invoke-WebRequest -UseBasicParsing -Uri $healthUrl -TimeoutSec 2
        return $resp.StatusCode -ge 200
    } catch {
        return $false
    }
}

if (Test-BackendHealth) {
    Write-Host "Backend already running on port 8080"
    exit 0
}

$mvnCmd = "C:\Users\XaviXPC\Documents\Tools\apache-maven-3.9.13\bin\mvn.cmd"
if (!(Test-Path $mvnCmd)) {
    $mvnCmd = "mvn"
}

$proc = Start-Process -FilePath $mvnCmd -ArgumentList "spring-boot:run" -WorkingDirectory $backendDir -PassThru -WindowStyle Hidden

$attempts = 0
while ($attempts -lt 60) {
    Start-Sleep -Seconds 1
    if (Test-BackendHealth) {
        Write-Host "Backend started successfully (PID: $($proc.Id))"
        exit 0
    }
    $attempts++
}

Write-Host "Backend failed to start within 60 seconds"
exit 1
