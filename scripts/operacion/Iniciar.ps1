param([int]$Puerto = 8080, [int]$PuertoMonitoreo = 8081, [string]$BaseVerificacion = '')
. "$PSScriptRoot/Comun.ps1"
$config = Leer-Configuracion
$archivoPid = Join-Path $script:Local 'aplicacion.pid'
if (Test-Path -LiteralPath $archivoPid) {
    $pidAnterior = [int](Get-Content -LiteralPath $archivoPid)
    if (Get-Process -Id $pidAnterior -ErrorAction SilentlyContinue) { throw 'Existe un proceso registrado; verifique antes de volver a iniciar.' }
}
$jarConstruido = Join-Path $script:Raiz 'target/stockflow-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jarConstruido)) { throw 'Genere primero el JAR mediante mvnw.cmd verify.' }
$hashJar = (Get-FileHash -LiteralPath $jarConstruido -Algorithm SHA256).Hash
$versionLocal = Join-Path $script:Local "releases/$hashJar"
New-Item -ItemType Directory -Force -Path $versionLocal | Out-Null
$jar = Join-Path $versionLocal 'stockflow-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jar)) { Copy-Item -LiteralPath $jarConstruido -Destination $jar }
$env:SERVER_PORT = "$Puerto"
$env:MANAGEMENT_PORT = "$PuertoMonitoreo"
$env:STOCKFLOW_LOG_FILE = Join-Path $script:Local 'stockflow.jsonl'
if (!$BaseVerificacion -and $config.PSObject.Properties.Name -contains 'UsuarioAplicacion') {
    $env:DB_USERNAME = $config.UsuarioAplicacion
    $env:DB_PASSWORD = [Net.NetworkCredential]::new('', $config.PasswordAplicacion).Password
}
if ($BaseVerificacion) {
    if ($BaseVerificacion -notmatch '^stockflow_restore_[0-9]+$') { throw 'La verificación solo admite una base restaurada separada.' }
    $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/${BaseVerificacion}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima"
}
$proceso = Start-Process -FilePath 'java' -ArgumentList @('-jar', "`"$jar`"", '--spring.profiles.active=operacion') -WorkingDirectory $script:Raiz -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:Local 'stdout.log') -RedirectStandardError (Join-Path $script:Local 'stderr.log')
$proceso.Id | Set-Content -LiteralPath $archivoPid
Guardar-Evento 'inicio' 'INFO' @{pid=$proceso.Id; puerto=$Puerto; monitoreo=$PuertoMonitoreo}
@{sha256=$hashJar; pid=$proceso.Id; puerto=$Puerto; monitoreo=$PuertoMonitoreo; baseVerificacion=$BaseVerificacion; fecha=[DateTimeOffset]::Now.ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $script:Local 'despliegue.json') -Encoding utf8
Write-Output "Iniciada; PID $($proceso.Id). Web: http://127.0.0.1:$Puerto; monitoreo local: $PuertoMonitoreo. Compruebe salud antes de usarla."
