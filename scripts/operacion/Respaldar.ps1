param([ValidateRange(1,365)][int]$RetencionDias = 14)
. "$PSScriptRoot/Comun.ps1"
$config = Leer-Configuracion
Validar-Base $config.Base
$destino = Join-Path $script:Local 'backups'
New-Item -ItemType Directory -Force -Path $destino | Out-Null
$marca = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$archivo = Join-Path $destino "$($config.Base)-$marca.sql"
$temporal = "$archivo.partial"
$inicio = [Diagnostics.ProcessStartInfo]::new((Join-Path $config.MySqlBin 'mysqldump.exe'))
$inicio.UseShellExecute = $false
$inicio.CreateNoWindow = $true
$inicio.RedirectStandardError = $true
$inicio.Environment['MYSQL_PWD'] = $env:DB_PASSWORD
foreach ($arg in @('--host=127.0.0.1', "--user=$($config.Usuario)", '--single-transaction', '--skip-lock-tables', '--no-tablespaces', '--set-gtid-purged=OFF', '--column-statistics=0', '--default-character-set=utf8mb4', "--result-file=$temporal", $config.Base)) {
    $inicio.ArgumentList.Add($arg)
}
$proceso = [Diagnostics.Process]::Start($inicio)
$errores = $proceso.StandardError.ReadToEnd()
$proceso.WaitForExit()
if ($proceso.ExitCode -ne 0 -or !(Test-Path -LiteralPath $temporal) -or (Get-Item -LiteralPath $temporal).Length -eq 0) {
    Guardar-Evento 'backup' 'ERROR' @{base=$config.Base; resultado='fallido'}
    throw "El respaldo falló; no se elimina ningún respaldo anterior. $errores"
}
Move-Item -LiteralPath $temporal -Destination $archivo
$manifiesto = @{archivo=[IO.Path]::GetFileName($archivo); base=$config.Base; fecha=[DateTimeOffset]::Now.ToString('o'); bytes=(Get-Item -LiteralPath $archivo).Length; sha256=(Get-FileHash -LiteralPath $archivo -Algorithm SHA256).Hash}
$manifiesto | ConvertTo-Json | Set-Content -LiteralPath "$archivo.json" -Encoding utf8
# Retención únicamente de dumps completos de esta base, después de un backup exitoso.
Get-ChildItem -LiteralPath $destino -File -Filter "$($config.Base)-*.sql" | Where-Object {$_.LastWriteTime -lt (Get-Date).AddDays(-$RetencionDias)} | ForEach-Object {
    Remove-Item -LiteralPath $_.FullName
    if (Test-Path -LiteralPath "$($_.FullName).json") { Remove-Item -LiteralPath "$($_.FullName).json" }
}
Guardar-Evento 'backup' 'INFO' @{base=$config.Base; archivo=$manifiesto.archivo; bytes=$manifiesto.bytes; sha256=$manifiesto.sha256}
Write-Output $archivo
