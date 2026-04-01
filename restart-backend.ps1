$portLines = netstat -ano | Select-String ":8080"
$pids = @()

foreach ($line in $portLines) {
    $parts = ($line.ToString() -split '\s+') | Where-Object { $_ -ne '' }
    if ($parts.Length -ge 5) {
        $pidValue = $parts[-1]
        if ($pidValue -match '^\d+$') {
            $pids += [int]$pidValue
        }
    }
}

$pids = $pids | Select-Object -Unique
foreach ($procId in $pids) {
    Write-Host "Stopping PID $procId"
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Seconds 2
Write-Host "Starting backend..."
& "C:\Users\XaviXPC\Documents\Projects\amz-express-spring\start-backend.ps1"
