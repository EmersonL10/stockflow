# Requiere PowerShell 7. Los secretos se cifran con DPAPI para el usuario de Windows.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:Raiz = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$script:Local = Join-Path $script:Raiz 'operacion-local'

function Leer-Configuracion {
    $archivo = Join-Path $script:Local 'configuracion.xml'
    if (!(Test-Path -LiteralPath $archivo)) { throw 'Ejecute primero Configurar.ps1.' }
    $config = Import-Clixml -LiteralPath $archivo
    $env:DB_PASSWORD = [Net.NetworkCredential]::new('', $config.Password).Password
    $env:APP_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $config.AdminPassword).Password
    $env:APP_ADMIN_USERNAME = $config.AdminUsuario
    $env:DB_USERNAME = $config.Usuario
    $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/$($config.Base)?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima"
    return $config
}

function Ejecutar-MySql([string]$Sql, [string]$Base = '') {
    $config = Leer-Configuracion
    $exe = Join-Path $config.MySqlBin 'mysql.exe'
    $inicio = [Diagnostics.ProcessStartInfo]::new($exe)
    $inicio.UseShellExecute = $false
    $inicio.CreateNoWindow = $true
    $inicio.RedirectStandardOutput = $true
    $inicio.RedirectStandardError = $true
    $inicio.RedirectStandardInput = $true
    $inicio.Environment['MYSQL_PWD'] = $env:DB_PASSWORD
    foreach ($arg in @('--host=127.0.0.1', "--user=$($config.Usuario)", '--batch', '--skip-column-names', '--default-character-set=utf8mb4')) {
        $inicio.ArgumentList.Add($arg)
    }
    if ($Base) { $inicio.ArgumentList.Add("--database=$Base") }
    $proceso = [Diagnostics.Process]::Start($inicio)
    $proceso.StandardInput.WriteLine($Sql)
    $proceso.StandardInput.Close()
    $salida = $proceso.StandardOutput.ReadToEnd()
    $errores = $proceso.StandardError.ReadToEnd()
    $proceso.WaitForExit()
    if ($proceso.ExitCode -ne 0) { throw "MySQL no completó la operación: $errores" }
    return $salida.Trim()
}

function Validar-Base([string]$Nombre) {
    if ($Nombre -notmatch '^stockflow_[a-z0-9_]+$') { throw 'Nombre de base no permitido.' }
}

function Guardar-Evento([string]$Evento, [string]$Nivel, [hashtable]$Datos) {
    $registro = @{fecha = [DateTimeOffset]::Now.ToString('o'); evento = $Evento; nivel = $Nivel; datos = $Datos}
    $registro | ConvertTo-Json -Depth 6 -Compress | Add-Content -LiteralPath (Join-Path $script:Local 'operaciones.jsonl') -Encoding utf8
}
