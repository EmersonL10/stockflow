param([string]$Resultados = 'docs/evidencias/entrega-final-2026-11-26.json')
. "$PSScriptRoot/Comun.ps1"
$ruta = Join-Path $script:Raiz $Resultados
$datos = Get-Content -LiteralPath $ruta -Raw | ConvertFrom-Json
$filas = $datos.casos | ForEach-Object {
    [pscustomobject]@{Caso=$_.id; Objetivo=$_.descripcion; Resultado=$(if ($_.aprobado) {'APROBADO'} else {'REVISAR'}); Obtenido=($_.obtenido | ConvertTo-Json -Depth 6 -Compress)}
}
$totalJava = ($datos.pruebasJava | Measure-Object -Property pruebas -Sum).Sum
$encabezado = @"
<meta charset="utf-8"><style>body{font:16px Arial,sans-serif;max-width:1200px;margin:40px auto;padding:20px;color:#18283a}table{border-collapse:collapse;width:100%;font-size:14px}th,td{border:1px solid #ccd3da;padding:10px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#eef2f6}h1{font-size:28px}p{line-height:1.5}@media print{body{margin:0}tr{break-inside:avoid}}</style>
"@
$intro = "<h1>StockFlow: evidencia de verificación</h1><p>Fecha: $([Net.WebUtility]::HtmlEncode($datos.fecha)). Entorno: Windows local, Java 21 y MySQL.</p><p>$totalJava pruebas Java y $($datos.casos.Count) comprobaciones de entorno. Esta página resume resultados reales del JSON; no es una captura de la aplicación ni una calificación académica.</p>"
$html = $filas | ConvertTo-Html -Title 'StockFlow: resultados de verificación' -Head $encabezado -PreContent $intro -PostContent '<p>Consultar pruebas-seguridad.md, operacion.md, monitoreo.md y mantenimiento.md para procedimientos y límites. El respaldo se restauró en una base separada; no se publica el dump. El ensayo de carga se limita a lecturas del login.</p>'
$salida = [IO.Path]::ChangeExtension($ruta, '.html')
$html | Set-Content -LiteralPath $salida -Encoding utf8
Write-Output $salida
