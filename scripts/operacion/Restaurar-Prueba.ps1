param([Parameter(Mandatory)][string]$Archivo)
. "$PSScriptRoot/Comun.ps1"
$config = Leer-Configuracion
$archivoReal = (Resolve-Path -LiteralPath $Archivo).Path
$carpetaBackups = [IO.Path]::GetFullPath((Join-Path $script:Local 'backups')) + [IO.Path]::DirectorySeparatorChar
if (!$archivoReal.StartsWith($carpetaBackups, [StringComparison]::OrdinalIgnoreCase)) { throw 'Use un respaldo generado dentro de operacion-local/backups.' }
$manifiesto = Get-Content -LiteralPath "$archivoReal.json" -Raw | ConvertFrom-Json
if ((Get-FileHash -LiteralPath $archivoReal -Algorithm SHA256).Hash -ne $manifiesto.sha256) { throw 'El hash del respaldo no coincide. Restauración cancelada.' }
# Base nueva; nunca restaura sobre stockflow_db ni elimina bases existentes.
$basePrueba = 'stockflow_restore_' + (Get-Date -Format 'yyyyMMddHHmmssfff')
Ejecutar-MySql "CREATE DATABASE $basePrueba CHARACTER SET utf8mb4;" | Out-Null
$inicio = [Diagnostics.ProcessStartInfo]::new((Join-Path $config.MySqlBin 'mysql.exe'))
$inicio.UseShellExecute = $false
$inicio.CreateNoWindow = $true
$inicio.RedirectStandardInput = $true
$inicio.RedirectStandardError = $true
$inicio.Environment['MYSQL_PWD'] = $env:DB_PASSWORD
foreach ($arg in @('--host=127.0.0.1', "--user=$($config.Usuario)", '--default-character-set=utf8mb4', "--database=$basePrueba")) { $inicio.ArgumentList.Add($arg) }
$proceso = [Diagnostics.Process]::Start($inicio)
$errorTask = $proceso.StandardError.ReadToEndAsync()
$stream = [IO.File]::OpenRead($archivoReal)
try { $stream.CopyTo($proceso.StandardInput.BaseStream) } finally {$stream.Dispose(); $proceso.StandardInput.Close()}
$proceso.WaitForExit()
if ($proceso.ExitCode -ne 0) { throw "Falló la importación: $($errorTask.GetAwaiter().GetResult())" }
$tablas = Ejecutar-MySql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$basePrueba';"
$conteos = Ejecutar-MySql 'SELECT COUNT(*) FROM salidas; SELECT COUNT(*) FROM caja_movimientos; SELECT COUNT(*) FROM encomiendas;' $basePrueba
$resultado = @{baseRestaurada=$basePrueba; tablas=[int]$tablas; conteos=$conteos; sha256=$manifiesto.sha256; resultado='importacion_completa'; fecha=[DateTimeOffset]::Now.ToString('o')}
$resultado | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $script:Local 'restauracion.json') -Encoding utf8
Guardar-Evento 'restauracion_prueba' 'INFO' $resultado
Write-Output "Restauración completa en $basePrueba. Se conserva para inspección; base original intacta."
