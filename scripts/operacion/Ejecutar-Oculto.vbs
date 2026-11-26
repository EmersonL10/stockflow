' Lanzador de las tareas de StockFlow sin ventana de consola.
' Espera a PowerShell y conserva su código de salida para el Programador de tareas.
Option Explicit
Dim argumentos, comando, indice, shell, resultado
Set argumentos = WScript.Arguments
If argumentos.Count < 2 Then WScript.Quit 2

Function EntreComillas(valor)
    EntreComillas = Chr(34) & Replace(valor, Chr(34), Chr(34) & Chr(34)) & Chr(34)
End Function

comando = EntreComillas(argumentos(0)) & " -NoLogo -NoProfile -NonInteractive -WindowStyle Hidden -File " & EntreComillas(argumentos(1))
For indice = 2 To argumentos.Count - 1
    comando = comando & " " & EntreComillas(argumentos(indice))
Next
Set shell = CreateObject("WScript.Shell")
On Error Resume Next
resultado = shell.Run(comando, 0, True)
If Err.Number <> 0 Then WScript.Quit 3
On Error GoTo 0
WScript.Quit resultado
