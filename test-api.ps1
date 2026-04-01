$ErrorActionPreference = 'SilentlyContinue'
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/health' -UseBasicParsing
    Write-Host "Status:" $r.StatusCode
    Write-Host "Body:" $r.Content
} catch {
    Write-Host "Error:" $_.Exception.Message
    if ($_.Exception.Response) {
        Write-Host "Status Code:" $_.Exception.Response.StatusCode.value__
    }
}
