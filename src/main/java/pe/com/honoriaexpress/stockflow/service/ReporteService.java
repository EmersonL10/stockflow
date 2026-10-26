package pe.com.honoriaexpress.stockflow.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {
    public static final int MAX_FILAS = 10000;
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private final SalidaRepository salidas;
    private final EncomiendaRepository encomiendas;
    private final MovimientoCajaRepository movimientos;

    public record Fila(LocalDateTime fecha, String referencia, String detalle, String estado, BigDecimal importe) { }
    public record Indicador(String nombre, BigDecimal importe) { }
    public record Resultado(ModuloReporte modulo, LocalDate desde, LocalDate hasta, String criterioFecha,
                            List<Fila> filas, List<Indicador> indicadores) { }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO','CAJA')")
    public Resultado consultar(ModuloReporte modulo, LocalDate desde, LocalDate hasta) {
        comprobarPermiso(modulo);
        if (modulo == null || desde == null || hasta == null || desde.isAfter(hasta)
                || desde.getYear() < 1000 || hasta.getYear() > 9998) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique un período válido: Desde no puede ser posterior a Hasta.");
        }
        var limite = PageRequest.of(0, MAX_FILAS + 1);
        List<Fila> filas = new ArrayList<>();
        BigDecimal total = CERO, pagado = CERO, pendiente = CERO;
        String criterio;
        switch (modulo) {
            case SALIDAS -> {
                var registros = salidas.findByTurnoFechaBetweenOrderByTurnoFechaDescIdDesc(desde, hasta, limite);
                comprobarLimite(registros.size());
                for (Salida s : registros) {
                    var t = s.getTurno();
                    filas.add(new Fila(t.getFecha().atTime(s.getHoraSalidaReal()), "Salida #" + s.getId(),
                            t.getVehiculo().getPlaca() + " · " + t.getConductor().getNombres() + " "
                                    + t.getConductor().getApellidos() + " · " + s.getRutaMostrada(),
                            s.getEstado().getEtiqueta() + " · " + (s.getEstadoCuota() == null ? "Sin cuota" : s.getEstadoCuota().name()),
                            s.getCuotaAgencia()));
                    if (s.getEstado() != EstadoSalida.CANCELADA && s.getCuotaAgencia() != null
                            && s.getEstadoCuota() != EstadoCuota.ANULADA) {
                        total = total.add(s.getCuotaAgencia());
                        if (s.getEstadoCuota() == EstadoCuota.PAGADA) pagado = pagado.add(s.getCuotaAgencia());
                        if (s.getEstadoCuota() == EstadoCuota.PENDIENTE) pendiente = pendiente.add(s.getCuotaAgencia());
                    }
                }
                criterio = "Por fecha del viaje. Las cuotas pagadas pueden haberse cobrado en otra fecha.";
            }
            case ENCOMIENDAS -> {
                var registros = encomiendas.findByRegistradaEnGreaterThanEqualAndRegistradaEnLessThanOrderByRegistradaEnDescIdDesc(
                        desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), limite);
                comprobarLimite(registros.size());
                for (Encomienda e : registros) {
                    filas.add(new Fila(e.getRegistradaEn(), e.getCodigo(), e.getRuta() + " · " + e.getDescripcion(),
                            e.getEstado().name() + " · " + e.getEstadoPago().name(), e.getImporte()));
                    if (e.getEstado() != EstadoEncomienda.CANCELADA) {
                        total = total.add(e.getImporte());
                        if (e.getEstadoPago() == EstadoPagoEncomienda.PAGADA) pagado = pagado.add(e.getImporte());
                        if (e.getEstadoPago() == EstadoPagoEncomienda.PENDIENTE) pendiente = pendiente.add(e.getImporte());
                    }
                }
                criterio = "Por fecha de recepción del envío. Los importes pagados pueden haberse cobrado en otra fecha.";
            }
            case CAJA -> {
                var registros = movimientos.findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDescIdDesc(
                        desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), limite);
                comprobarLimite(registros.size());
                for (MovimientoCaja m : registros) {
                    filas.add(new Fila(m.getFecha(), "Caja #" + m.getCaja().getId() + " · Movimiento #" + m.getId(),
                            m.getConcepto(), m.getTipo().name() + " · " + m.getMedio().name(), m.getImporte()));
                    if (m.getTipo() == TipoMovimientoCaja.INGRESO) total = total.add(m.getImporte());
                    else pagado = pagado.add(m.getImporte());
                }
                pendiente = total.subtract(pagado);
                criterio = "Por fecha del movimiento, en hora de Lima. Incluye efectivo y digital; el saldo no incorpora fondos iniciales ni equivale al efectivo contado.";
            }
            default -> throw new IllegalArgumentException("Módulo no admitido");
        }
        var indicadores = modulo == ModuloReporte.CAJA
                ? List.of(new Indicador("Ingresos", total), new Indicador("Egresos", pagado), new Indicador("Ingresos menos egresos", pendiente))
                : List.of(new Indicador(modulo == ModuloReporte.SALIDAS ? "Cuotas generadas" : "Importe registrado", total),
                          new Indicador("Pagado", pagado), new Indicador("Pendiente", pendiente));
        return new Resultado(modulo, desde, hasta, criterio, List.copyOf(filas), indicadores);
    }

    private void comprobarPermiso(ModuloReporte modulo) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (modulo == ModuloReporte.CAJA && (auth == null || auth.getAuthorities().stream().noneMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRADOR") || a.getAuthority().equals("ROLE_CAJA")))) {
            throw new AccessDeniedException("El reporte de Caja requiere permiso de Caja o Administrador.");
        }
    }

    private void comprobarLimite(int cantidad) {
        if (cantidad > MAX_FILAS) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                "El período supera 10000 registros. Reduzca las fechas para consultar o exportar.");
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO','CAJA')")
    public byte[] exportar(Resultado resultado) throws IOException {
        comprobarPermiso(resultado.modulo());
        comprobarLimite(resultado.filas().size());
        try (var libro = new XSSFWorkbook(); var bytes = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet(resultado.modulo().getEtiqueta());
            String[] titulos = {"Fecha y hora", "Referencia", "Detalle", "Estado", "Importe"};
            var encabezado = hoja.createRow(0);
            for (int i = 0; i < titulos.length; i++) encabezado.createCell(i, CellType.STRING).setCellValue(titulos[i]);
            var moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("\"S/ \"#,##0.00"));
            int numero = 1;
            for (Fila f : resultado.filas()) {
                var fila = hoja.createRow(numero++);
                fila.createCell(0, CellType.STRING).setCellValue(f.fecha().toString());
                fila.createCell(1, CellType.STRING).setCellValue(f.referencia());
                fila.createCell(2, CellType.STRING).setCellValue(f.detalle());
                fila.createCell(3, CellType.STRING).setCellValue(f.estado());
                if (f.importe() != null) { var celda = fila.createCell(4); celda.setCellValue(f.importe().doubleValue()); celda.setCellStyle(moneda); }
            }
            hoja.createFreezePane(0, 1);
            if (numero > 1) hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, numero - 1, 0, 4));
            for (int i = 0; i < 5; i++) hoja.setColumnWidth(i, (i == 2 ? 65 : 30) * 256);
            var resumen = libro.createSheet("Resumen");
            resumen.createRow(0).createCell(0).setCellValue(resultado.modulo().getEtiqueta());
            resumen.createRow(1).createCell(0).setCellValue("Desde " + resultado.desde() + " hasta " + resultado.hasta());
            resumen.createRow(2).createCell(0).setCellValue(resultado.criterioFecha());
            resumen.createRow(3).createCell(0).setCellValue("Registros: " + resultado.filas().size());
            numero = 5;
            for (Indicador i : resultado.indicadores()) {
                var fila = resumen.createRow(numero++); fila.createCell(0).setCellValue(i.nombre());
                var celda = fila.createCell(1); celda.setCellValue(i.importe().doubleValue()); celda.setCellStyle(moneda);
            }
            resumen.setColumnWidth(0, 100 * 256); resumen.setColumnWidth(1, 25 * 256);
            libro.write(bytes);
            return bytes.toByteArray();
        }
    }
}
