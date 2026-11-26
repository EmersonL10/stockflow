param(
    [string]$Base = 'stockflow_db',
    [string]$Usuario = 'root',
    [string]$AdminUsuario = 'admin',
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
)
. "$PSScriptRoot/Comun.ps1"
Validar-Base $Base
if (!$env:DB_PASSWORD -or !$env:APP_ADMIN_PASSWORD) { throw 'Defina DB_PASSWORD y APP_ADMIN_PASSWORD en esta sesión; no las escriba en archivos versionados.' }
foreach ($exe in @('mysql.exe', 'mysqldump.exe')) {
    if (!(Test-Path -LiteralPath (Join-Path $MySqlBin $exe))) { throw "No se encontró $exe en MySqlBin." }
}
New-Item -ItemType Directory -Force -Path $script:Local | Out-Null
$identidad = [Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $script:Local /inheritance:r /grant:r "${identidad}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron restringir los permisos de operacion-local.' }
$config = [pscustomobject]@{
    Base = $Base; Usuario = $Usuario; AdminUsuario = $AdminUsuario; MySqlBin = $MySqlBin
    Password = ConvertTo-SecureString $env:DB_PASSWORD -AsPlainText -Force
    AdminPassword = ConvertTo-SecureString $env:APP_ADMIN_PASSWORD -AsPlainText -Force
}
$config | Export-Clixml -LiteralPath (Join-Path $script:Local 'configuracion.xml')
Write-Output "Configuración local cifrada para $identidad. Base: $Base."
