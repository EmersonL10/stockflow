param([int]$Puerto = 8081)
. "$PSScriptRoot/Comun.ps1"
$config = Leer-Configuracion
$baseUrl = "http://127.0.0.1:$Puerto/actuator"
$credencial = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($config.AdminUsuario):$env:APP_ADMIN_PASSWORD"))
$headers = @{Authorization="Basic $credencial"}
$reloj = [Diagnostics.Stopwatch]::StartNew()
try {
    $salud = Invoke-RestMethod "$baseUrl/health" -TimeoutSec 10
    $medidas = @{}
    foreach ($nombre in @('process.uptime','jvm.memory.used','process.cpu.usage','http.server.requests','stockflow.cuotas.pendientes','stockflow.vehiculos.activos','stockflow.cajas.abiertas')) {
        try {
            $medidas[$nombre] = (Invoke-RestMethod "$baseUrl/metrics/$nombre" -Headers $headers -TimeoutSec 10).measurements
        } catch {
            if ($nombre -eq 'http.server.requests' -and $_.Exception.Response.StatusCode.value__ -eq 404) {
                $medidas[$nombre] = @{estado='SIN_SOLICITUDES_WEB_REGISTRADAS'}
            } else { throw }
        }
    }
    $reloj.Stop()
    $resultado = @{estado=$salud.status; duracionMs=$reloj.ElapsedMilliseconds; metricas=$medidas; fecha=[DateTimeOffset]::Now.ToString('o')}
    $resultado | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $script:Local 'monitoreo-ultimo.json') -Encoding utf8
    $nivel = if ($reloj.ElapsedMilliseconds -gt 5000) {'WARN'} else {'INFO'}
    Guardar-Evento 'monitoreo' $nivel $resultado
    if ($salud.status -ne 'UP') { throw 'Salud distinta de UP.' }
    Write-Output "UP; métricas consultadas: $($medidas.Count); duración del muestreo: $($reloj.ElapsedMilliseconds) ms."
} catch {
    Guardar-Evento 'monitoreo' 'ERROR' @{estado='DOWN_O_INCOMPLETO'; puerto=$Puerto}
    throw
}
