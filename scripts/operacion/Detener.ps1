. "$PSScriptRoot/Comun.ps1"
$archivoPid = Join-Path $script:Local 'aplicacion.pid'
$identificador = [int](Get-Content -LiteralPath $archivoPid)
$proceso = Get-CimInstance Win32_Process -Filter "ProcessId=$identificador"
if ($proceso) {
    if ($proceso.Name -notmatch '^java(w)?\.exe$' -or $proceso.CommandLine -notlike '*stockflow-0.0.1-SNAPSHOT.jar*' -or $proceso.CommandLine -notlike '*spring.profiles.active=operacion*') {
        throw 'El PID no corresponde a esta aplicación. No se detiene.'
    }
    Stop-Process -Id $identificador
}
Remove-Item -LiteralPath $archivoPid
Guardar-Evento 'parada' 'INFO' @{pid=$identificador}
