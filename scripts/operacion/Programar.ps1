param([switch]$Quitar, [int]$PuertoMonitoreo = 8081)
. "$PSScriptRoot/Comun.ps1"
$null = Leer-Configuracion
$tareas = @('StockFlow-BackupDiario', 'StockFlow-Monitoreo')
if ($Quitar) {
    foreach ($nombre in $tareas) { Unregister-ScheduledTask -TaskName $nombre -Confirm:$false -ErrorAction SilentlyContinue }
    Write-Output 'Tareas retiradas; no se borraron respaldos ni configuración.'
    return
}
$pwsh = (Get-Process -Id $PID).Path
$usuario = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$principal = New-ScheduledTaskPrincipal -UserId $usuario -LogonType Interactive -RunLevel Limited
$ajustes = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 20)
$diario = New-ScheduledTaskTrigger -Daily -At '23:30'
$cadaCinco = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 5)
$lanzador = Join-Path $env:SystemRoot 'System32/wscript.exe'
$vbs = Join-Path $PSScriptRoot 'Ejecutar-Oculto.vbs'
$backup = New-ScheduledTaskAction -Execute $lanzador -Argument "//B //NoLogo `"$vbs`" `"$pwsh`" `"$PSScriptRoot\Respaldar.ps1`"" -WorkingDirectory $script:Raiz
$monitor = New-ScheduledTaskAction -Execute $lanzador -Argument "//B //NoLogo `"$vbs`" `"$pwsh`" `"$PSScriptRoot\Monitorear.ps1`" -Puerto $PuertoMonitoreo" -WorkingDirectory $script:Raiz
Register-ScheduledTask -TaskName $tareas[0] -Action $backup -Trigger $diario -Principal $principal -Settings $ajustes -Description 'Backup MySQL con manifiesto SHA256 y retención de 14 días.' -Force | Out-Null
Register-ScheduledTask -TaskName $tareas[1] -Action $monitor -Trigger $cadaCinco -Principal $principal -Settings $ajustes -Description 'Salud, rendimiento y métricas de StockFlow cada cinco minutos.' -Force | Out-Null
Write-Output 'Tareas registradas para el usuario actual. Requieren sesión iniciada; no se ejecutan con el equipo apagado.'
