package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.service.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteSalidasTests {
    final ConsultaSalidasService consulta = new ConsultaSalidasService();
    final ExportacionSalidasService exportacion = new ExportacionSalidasService(consulta);
    final LocalDate dia = LocalDate.of(2026, 10, 24);

    Salida salida(long id, String ruta, EstadoCuota estado, BigDecimal cuota, LocalDate fecha) {
        var t = Turno.builder().fecha(fecha).sentidoRuta(SentidoRuta.PUCALLPA_HONORIA)
                .vehiculo(Vehiculo.builder().placa("A4P-410").build())
                .conductor(Conductor.builder().nombres("Conductor").apellidos("Prueba").build()).build();
        return Salida.builder().id(id).turno(t).rutaReal(ruta).horaSalidaReal(LocalTime.NOON)
                .cantidadPasajeros(4).estado(EstadoSalida.EN_RUTA).estadoCuota(estado).cuotaAgencia(cuota).build();
    }

    @Test void filtrosCombinadosNormalizanEspaciosSinModificarDatos() {
        var a = salida(1, "Pucallpa   - Honoria", EstadoCuota.PENDIENTE, BigDecimal.TEN, dia);
        var pagada = salida(2, "Pucallpa - Honoria", EstadoCuota.PAGADA, BigDecimal.TEN, dia);
        var ayer = salida(3, "Pucallpa - Honoria", EstadoCuota.PENDIENTE, BigDecimal.TEN, dia.minusDays(1));
        assertEquals(List.of(a), consulta.filtrar(List.of(a, pagada, ayer), dia,
                "  pucallpa - honoria  ", " a4p ", EstadoCuota.PENDIENTE, false));
        assertEquals(2, consulta.filtrar(List.of(a, pagada, ayer), dia,
                null, null, EstadoCuota.PENDIENTE, true).size());
        assertEquals("Pucallpa   - Honoria", a.getRutaReal());
    }

    @Test void excelDistinguePendientesPagadasAnuladasEHistoricosSinEjecutarFormulas() throws Exception {
        var registros = List.of(
                salida(1, "=1+1", EstadoCuota.PAGADA, BigDecimal.TEN, dia),
                salida(2, "=1+1", EstadoCuota.PENDIENTE, BigDecimal.TEN, dia),
                salida(3, "=1+1", EstadoCuota.ANULADA, BigDecimal.TEN, dia),
                salida(4, "Otra ruta", null, null, dia));
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(exportacion.exportar(registros)))) {
            assertEquals(3, libro.getNumberOfSheets());
            var detalle = libro.getSheet("Salidas");
            assertEquals(5, detalle.getPhysicalNumberOfRows());
            assertEquals(CellType.STRING, detalle.getRow(1).getCell(2).getCellType());
            assertEquals("=1+1", detalle.getRow(1).getCell(2).getStringCellValue());
            assertEquals(CellType.NUMERIC, detalle.getRow(1).getCell(7).getCellType());
            assertNull(detalle.getRow(4).getCell(7));
            var resumen = libro.getSheet("Resumen por ruta");
            assertEquals(10.0, resumen.getRow(1).getCell(2).getNumericCellValue());
            assertEquals(10.0, resumen.getRow(1).getCell(3).getNumericCellValue());
            assertEquals(1.0, resumen.getRow(2).getCell(4).getNumericCellValue());
            assertEquals(2, consulta.agruparPorRuta(registros).keySet().size());
        }
    }

    @Test void excelVacioTieneEncabezadosYSinTotalesInventados() throws Exception {
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(exportacion.exportar(List.of())))) {
            assertEquals(1, libro.getSheet("Salidas").getPhysicalNumberOfRows());
            assertEquals(1, libro.getSheet("Resumen por ruta").getPhysicalNumberOfRows());
        }
    }

    @Test void rechazaExportacionExcesivaAntesDeConstruirLibro() {
        assertThrows(IllegalArgumentException.class, () -> exportacion.exportar(
                Collections.nCopies(ExportacionSalidasService.MAX_FILAS + 1, new Salida())));
    }
}
