# La cuenta de aplicación no administra esquemas ni usuarios. El backup usa la cuenta de mantenimiento.
. "$PSScriptRoot/Comun.ps1"
$config = Leer-Configuracion
Validar-Base $config.Base
$usuario = 'stockflow_app'
$existe = Ejecutar-MySql "SELECT COUNT(*) FROM mysql.user WHERE User='$usuario' AND Host='127.0.0.1';"
if ($existe -ne '0') { throw 'stockflow_app ya existe. No se cambia su contraseña ni sus permisos automáticamente.' }
$passwordApp = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
Ejecutar-MySql "CREATE USER '$usuario'@'127.0.0.1' IDENTIFIED BY '$passwordApp'; GRANT SELECT, INSERT, UPDATE, DELETE ON $($config.Base).* TO '$usuario'@'127.0.0.1';" | Out-Null
$config | Add-Member -NotePropertyName UsuarioAplicacion -NotePropertyValue $usuario -Force
$config | Add-Member -NotePropertyName PasswordAplicacion -NotePropertyValue (ConvertTo-SecureString $passwordApp -AsPlainText -Force) -Force
$config | Export-Clixml -LiteralPath (Join-Path $script:Local 'configuracion.xml')
Guardar-Evento 'usuario_aplicacion' 'INFO' @{usuario=$usuario; base=$config.Base; permisos='SELECT INSERT UPDATE DELETE'}
Write-Output 'Cuenta limitada creada; contraseña almacenada cifrada. Reinicie el JAR para utilizarla.'
