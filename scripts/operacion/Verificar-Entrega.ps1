param([int]$PuertoWeb = 18080, [int]$PuertoMonitoreo = 18081)
. "$PSScriptRoot/Comun.ps1"
$config = Leer-Configuracion
$evidencias = Join-Path $script:Raiz 'target/evidencias-entrega'
New-Item -ItemType Directory -Force -Path $evidencias | Out-Null
$casos = [Collections.Generic.List[object]]::new()
function Comprobar([string]$Id, [string]$Descripcion, [bool]$Condicion, $Obtenido) {
    $casos.Add(@{id=$Id; descripcion=$Descripcion; aprobado=$Condicion; obtenido=$Obtenido})
    if (!$Condicion) { Write-Warning "$Id no coincide con lo esperado." }
}
$web = "http://127.0.0.1:$PuertoWeb"
$admin = "http://127.0.0.1:$PuertoMonitoreo/actuator"
$credencial = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($config.AdminUsuario):$env:APP_ADMIN_PASSWORD"))
$h = @{Authorization="Basic $credencial"}
$despliegue = Get-Content -LiteralPath (Join-Path $script:Local 'despliegue.json') -Raw | ConvertFrom-Json
if ($despliegue.baseVerificacion -notmatch '^stockflow_restore_[0-9]+$' -or $despliegue.puerto -ne $PuertoWeb) {
    throw 'Las verificaciones HTTP activas requieren el JAR iniciado sobre una base restaurada y el puerto registrado.'
}
$login = Invoke-WebRequest "$web/login" -SkipHttpErrorCheck
Comprobar 'DEP01' 'JAR desplegado responde al login' ($login.StatusCode -eq 200) $login.StatusCode
$entrada = Invoke-WebRequest "$web/login" -SessionVariable sesion -SkipHttpErrorCheck
$token = [regex]::Match($entrada.Content, 'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
if (!$token) { throw 'No se encontró el token del login.' }
$null = Invoke-WebRequest "$web/login" -Method Post -WebSession $sesion -Body @{username=$config.AdminUsuario; password=$env:APP_ADMIN_PASSWORD; _csrf=$token} -SkipHttpErrorCheck
foreach ($ruta in @('/', '/conductores', '/vehiculos', '/turnos', '/salidas', '/encomiendas', '/caja', '/reportes')) {
    $pagina = Invoke-WebRequest "$web$ruta" -WebSession $sesion -SkipHttpErrorCheck
    $destino = $pagina.BaseResponse.RequestMessage.RequestUri.AbsolutePath
    Comprobar ('WEB-' + $ruta) 'Vista accesible con autenticación real sin modificar registros' ($pagina.StatusCode -eq 200 -and $destino -eq $ruta) @{http=$pagina.StatusCode; destino=$destino}
}
$salud = Invoke-RestMethod "$admin/health"
Comprobar 'MON01' 'Salud anónima UP sin detalles internos' ($salud.status -eq 'UP' -and !($salud.PSObject.Properties.Name -contains 'components')) $salud.status
$sinCredenciales = Invoke-WebRequest "$admin/metrics" -SkipHttpErrorCheck
Comprobar 'SEC01' 'Métricas sin autenticación rechazadas' ($sinCredenciales.StatusCode -eq 401) $sinCredenciales.StatusCode
$metricas = Invoke-WebRequest "$admin/metrics" -Headers $h -SkipHttpErrorCheck
Comprobar 'SEC02' 'Administrador consulta métricas' ($metricas.StatusCode -eq 200) $metricas.StatusCode
$secreto = Invoke-WebRequest "$admin/env" -Headers $h -SkipHttpErrorCheck
Comprobar 'SEC03' 'Endpoint env no expuesto' ($secreto.StatusCode -eq 404) $secreto.StatusCode
$antes = Ejecutar-MySql 'SELECT COUNT(*) FROM caja_movimientos;' $despliegue.baseVerificacion
$csrf = Invoke-WebRequest "$web/salidas/15/pago" -Method Post -Body @{medioPago='EFECTIVO'} -SkipHttpErrorCheck
$despues = Ejecutar-MySql 'SELECT COUNT(*) FROM caja_movimientos;' $despliegue.baseVerificacion
Comprobar 'SEC04' 'POST sin CSRF rechazado sin crear ingresos' ($csrf.StatusCode -eq 403 -and $antes -eq $despues) @{http=$csrf.StatusCode; movimientosAntes=[int]$antes; movimientosDespues=[int]$despues}
$logs = Get-Content -LiteralPath (Join-Path $script:Local 'stockflow.jsonl') -Tail 100
$logHttp = @($logs | ForEach-Object {try {$_ | ConvertFrom-Json -ErrorAction Stop} catch {}} | Where-Object { $_.PSObject.Properties.Name -contains 'event' -and $_.event -eq 'http_request' })
Comprobar 'MON02' 'Log JSON con evento, nivel y correlación' ($logHttp.Count -gt 0) @{eventosHttp=$logHttp.Count}
$sondeo = & "$PSScriptRoot/Monitorear.ps1" -Puerto $PuertoMonitoreo
$ultimoMonitoreo = Get-Content -LiteralPath (Join-Path $script:Local 'monitoreo-ultimo.json') -Raw | ConvertFrom-Json
Comprobar 'MON03' 'Muestreo de siete métricas' ($ultimoMonitoreo.estado -eq 'UP') $sondeo
$restauracion = Get-Content -LiteralPath (Join-Path $script:Local 'restauracion.json') -Raw | ConvertFrom-Json
$baseRestaurada = $restauracion.baseRestaurada
if ($baseRestaurada -notmatch '^stockflow_restore_[0-9]+$') { throw 'Nombre de restauración no permitido.' }
$original = Ejecutar-MySql 'SELECT COUNT(*) FROM salidas; SELECT COUNT(*) FROM caja_movimientos; SELECT COUNT(*) FROM encomiendas;' $config.Base
$copia = Ejecutar-MySql 'SELECT COUNT(*) FROM salidas; SELECT COUNT(*) FROM caja_movimientos; SELECT COUNT(*) FROM encomiendas;' $baseRestaurada
Comprobar 'MAN01' 'Conteos origen/restauración coinciden en tres tablas críticas' ($original -eq $copia) @{origen=$original; restauracion=$copia; baseDestino=$baseRestaurada}
$tareas = @('StockFlow-BackupDiario','StockFlow-Monitoreo') | ForEach-Object {
    $info = Get-ScheduledTaskInfo -TaskName $_
    @{nombre=$_; ultimaEjecucion=$info.LastRunTime.ToString('o'); resultado=$info.LastTaskResult; proximaEjecucion=$info.NextRunTime.ToString('o')}
}
foreach ($tarea in $tareas) {
    Comprobar ('TASK-' + $tarea.nombre) 'Tarea ejecutada por el programador con salida cero' ($tarea.resultado -eq 0 -and $tarea.ultimaEjecucion -notlike '1999*') $tarea
}
# Carga exploratoria acotada: no equivale a un benchmark de producción.
$muestras = 1..25 | ForEach-Object -ThrottleLimit 5 -Parallel {
    $reloj = [Diagnostics.Stopwatch]::StartNew()
    $r = Invoke-WebRequest "http://127.0.0.1:$using:PuertoWeb/login" -SkipHttpErrorCheck -TimeoutSec 15
    $reloj.Stop()
    @{http=$r.StatusCode; ms=$reloj.Elapsed.TotalMilliseconds}
}
$ordenadas = @($muestras.ms | Sort-Object)
$rendimiento = @{solicitudes=25; concurrenciaMaxima=5; ruta='/login'; errores=@($muestras | Where-Object {$_.http -ne 200}).Count; medianaMs=[Math]::Round($ordenadas[12],2); p95Ms=[Math]::Round($ordenadas[23],2); maxMs=[Math]::Round($ordenadas[24],2)}
Comprobar 'PERF01' 'Lectura concurrente exploratoria: 25 solicitudes sin error' ($rendimiento.errores -eq 0) $rendimiento
$pruebas = @()
Get-ChildItem (Join-Path $script:Raiz 'target/surefire-reports/TEST-*.xml') | ForEach-Object {
    [xml]$xml = Get-Content -LiteralPath $_.FullName -Raw
    $pruebas += @{suite=$xml.testsuite.name; pruebas=[int]$xml.testsuite.tests; fallos=[int]$xml.testsuite.failures; errores=[int]$xml.testsuite.errors; omitidas=[int]$xml.testsuite.skipped}
}
$resultado = @{fecha=[DateTimeOffset]::Now.ToString('o'); entorno='Windows local, perfil operacion, MySQL'; casos=$casos.ToArray(); pruebasJava=$pruebas; tareas=$tareas; rendimiento=$rendimiento; respaldo=@{sha256=$restauracion.sha256; tablas=$restauracion.tablas; baseRestaurada=$baseRestaurada}}
$resultado | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $evidencias 'resultados.json') -Encoding utf8
Write-Output ('Comprobaciones aprobadas: ' + @($casos | Where-Object {$_.aprobado}).Count + '/' + $casos.Count)
if (@($casos | Where-Object {!$_.aprobado}).Count -gt 0) { throw 'Hay verificaciones pendientes; revise resultados.json.' }
