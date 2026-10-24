package pe.com.honoriaexpress.stockflow.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportacionSalidasService {
    public static final int MAX_FILAS = 10000;
    private static final Logger log = LoggerFactory.getLogger(ExportacionSalidasService.class);
    private final ConsultaSalidasService consulta;

    public byte[] exportar(List<Salida> registros) throws IOException {
        if (registros.size() > MAX_FILAS) {
            throw new IllegalArgumentException("El reporte supera 10000 salidas. Reduzca el período o aplique filtros.");
        }
        try (var libro = new XSSFWorkbook(); var bytes = new ByteArrayOutputStream()) {
            CellStyle cabecera = libro.createCellStyle();
            Font negrita = libro.createFont(); negrita.setBold(true);
            cabecera.setFont(negrita);
            cabecera.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("\"S/ \"#,##0.00"));
            Sheet detalle = libro.createSheet("Salidas");
            encabezado(detalle, cabecera, "ID", "Fecha de salida", "Ruta", "Placa", "Conductor",
                    "Hora real", "Pasajeros", "Cuota de agencia", "Estado de pago", "Fecha de pago", "Estado de salida");
            int fila = 1;
            for (Salida s : registros) {
                Row r = detalle.createRow(fila++);
                texto(r, 0, String.valueOf(s.getId()));
                texto(r, 1, s.getTurno().getFecha().toString());
                texto(r, 2, s.getRutaMostrada());
                texto(r, 3, s.getTurno().getVehiculo().getPlaca());
                texto(r, 4, s.getTurno().getConductor().getNombres() + " " + s.getTurno().getConductor().getApellidos());
                texto(r, 5, s.getHoraSalidaReal().toString());
                r.createCell(6).setCellValue(s.getCantidadPasajeros());
                if (s.getCuotaAgencia() != null) dinero(r, 7, s.getCuotaAgencia(), moneda);
                texto(r, 8, s.getEstadoCuota() == null ? "SIN CUOTA DETERMINADA" : s.getEstadoCuota().name());
                texto(r, 9, s.getFechaPago() == null ? "" : s.getFechaPago().toString());
                texto(r, 10, s.getEstado().name());
            }
            formato(detalle, 11, registros.size());
            Sheet resumen = libro.createSheet("Resumen por ruta");
            encabezado(resumen, cabecera, "Ruta", "Registros", "Cuotas pagadas", "Cuotas pendientes", "Sin cuota determinada");
            fila = 1;
            var grupos = consulta.agruparPorRuta(registros);
            for (String ruta : grupos.keySet()) {
                var salidas = grupos.get(ruta);
                Row r = resumen.createRow(fila++);
                texto(r, 0, ruta); r.createCell(1).setCellValue(salidas.size());
                dinero(r, 2, sumar(salidas, EstadoCuota.PAGADA), moneda);
                dinero(r, 3, sumar(salidas, EstadoCuota.PENDIENTE), moneda);
                r.createCell(4).setCellValue(salidas.stream().filter(s -> s.getCuotaAgencia() == null).count());
            }
            formato(resumen, 5, grupos.keySet().size());
            Sheet alcance = libro.createSheet("Lectura del reporte");
            texto(alcance.createRow(0), 0, "El reporte contiene únicamente las salidas que coinciden con los filtros del listado.");
            texto(alcance.createRow(1), 0, "Cuotas pagadas agrupa por fecha de salida; no equivale a cobros del día ni a un cierre de Caja.");
            texto(alcance.createRow(2), 0, "No se suma la recaudación del transportista. Cuotas anuladas no se incluyen en pagadas ni pendientes.");
            texto(alcance.createRow(3), 0, "Fechas y horas de pago registradas según America/Lima. Históricos sin cuota no generan deuda automática.");
            alcance.setColumnWidth(0, 120 * 256);
            libro.write(bytes);
            log.info("Exportación de salidas completada registros={} rutas={}", registros.size(), grupos.keySet().size());
            return bytes.toByteArray();
        }
    }

    private BigDecimal sumar(List<Salida> salidas, EstadoCuota estado) {
        return salidas.stream().filter(s -> s.getEstadoCuota() == estado && s.getCuotaAgencia() != null)
                .map(Salida::getCuotaAgencia).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void texto(Row fila, int columna, String valor) {
        // Siempre STRING, incluso si empieza con =, +, - o @. Nunca crear fórmulas desde datos del usuario.
        fila.createCell(columna, CellType.STRING).setCellValue(valor == null ? "" : valor);
    }
    private void dinero(Row fila, int columna, BigDecimal valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna); celda.setCellValue(valor.doubleValue()); celda.setCellStyle(estilo);
    }
    private void encabezado(Sheet hoja, CellStyle estilo, String... nombres) {
        Row r = hoja.createRow(0);
        for (int i = 0; i < nombres.length; i++) { texto(r, i, nombres[i]); r.getCell(i).setCellStyle(estilo); }
    }
    private void formato(Sheet hoja, int columnas, int filas) {
        hoja.createFreezePane(0, 1);
        if (filas > 0) hoja.setAutoFilter(new CellRangeAddress(0, filas, 0, columnas - 1));
        for (int i = 0; i < columnas; i++) hoja.setColumnWidth(i, (i == 2 || i == 4 ? 34 : 23) * 256);
    }
}
