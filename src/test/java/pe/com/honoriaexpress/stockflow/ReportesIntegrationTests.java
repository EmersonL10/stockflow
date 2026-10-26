package pe.com.honoriaexpress.stockflow;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@WithMockUser(roles = "ADMINISTRADOR")
class ReportesIntegrationTests {
    @Autowired ReporteService reportes;
    @Autowired InicioService inicio;
    @Autowired VehiculoService vehiculoService;
    @Autowired CajaService cajaService;
    @Autowired ConductorRepository conductores;
    @Autowired VehiculoRepository vehiculos;
    @Autowired TurnoRepository turnos;
    @Autowired SalidaRepository salidas;
    @Autowired EncomiendaRepository encomiendas;
    @Autowired EncomiendaEventoRepository eventos;
    @Autowired MovimientoCajaRepository movimientos;
    @Autowired CajaSesionRepository cajas;
    @Autowired MockMvc mvc;
    Conductor conductor;
    Vehiculo vehiculo;
    final LocalDate desde = LocalDate.of(2026, 9, 1), hasta = LocalDate.of(2026, 9, 3);

    @BeforeEach void preparar() {
        eventos.deleteAll(); movimientos.deleteAll(); encomiendas.deleteAll(); cajas.deleteAll();
        salidas.deleteAll(); turnos.deleteAll(); vehiculos.deleteAll(); conductores.deleteAll();
        conductor = conductores.saveAndFlush(Conductor.builder().dni("00000018").nombres("Prueba")
                .apellidos("Reportes").licencia("TEST-REPORTES").build());
        vehiculo = vehiculos.saveAndFlush(Vehiculo.builder().placa("QA-R01").marca("PRUEBA")
                .modelo("REPORTES").anio(2020).color("BLANCO").capacidadPasajeros(5).numeroTuc("QA-REPORTES").build());
    }

    private Salida viaje(LocalDate fecha, int orden, EstadoCuota cuota) {
        var turno = turnos.saveAndFlush(Turno.builder().fecha(fecha).numeroTurno(orden)
                .sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).horaProgramada(LocalTime.NOON)
                .conductor(conductor).vehiculo(vehiculo).estado(EstadoTurno.COMPLETADO).build());
        return salidas.saveAndFlush(Salida.builder().turno(turno).horaSalidaReal(LocalTime.NOON)
                .cantidadPasajeros(4).estado(EstadoSalida.FINALIZADA).estadoCuota(cuota)
                .cuotaAgencia(new BigDecimal("10.00")).build());
    }

    private Encomienda paquete(LocalDateTime fecha, EstadoEncomienda estado, EstadoPagoEncomienda pago) {
        var e = new Encomienda();
        e.setCodigo("ENC-" + UUID.randomUUID().toString().substring(0, 8)); e.setSolicitud(UUID.randomUUID().toString());
        e.setRemitente("Prueba"); e.setDocumentoRemitente("00000001"); e.setTelefonoRemitente("900000001");
        e.setDestinatario("Prueba destino"); e.setDocumentoDestinatario("00000002"); e.setTelefonoDestinatario("900000002");
        e.setRuta("Honoria - Pucallpa"); e.setDescripcion("Paquete de prueba"); e.setBultos(1);
        e.setImporte(new BigDecimal("25.00")); e.setEstado(estado); e.setEstadoPago(pago); e.setRegistradaEn(fecha);
        return encomiendas.saveAndFlush(e);
    }

    private void movimiento(CajaSesion caja, LocalDateTime fecha, TipoMovimientoCaja tipo, MedioPago medio, String importe) {
        var m = new MovimientoCaja(); m.setCaja(caja); m.setFecha(fecha); m.setTipo(tipo); m.setMedio(medio);
        m.setImporte(new BigDecimal(importe)); m.setConcepto("=texto de prueba"); m.setRegistradoPor("prueba");
        m.setSolicitud(UUID.randomUUID().toString()); movimientos.saveAndFlush(m);
    }

    @Test void rangoSalidasIncluyeAmbosExtremosYNoUsaFechaDePago() throws Exception {
        viaje(desde.minusDays(1), 1, EstadoCuota.PAGADA);
        var primera = viaje(desde, 1, EstadoCuota.PAGADA);
        primera.setFechaPago(hasta.plusDays(2).atStartOfDay()); salidas.saveAndFlush(primera);
        viaje(hasta, 1, EstadoCuota.PENDIENTE);
        viaje(hasta.plusDays(1), 1, EstadoCuota.PENDIENTE);
        var r = reportes.consultar(ModuloReporte.SALIDAS, desde, hasta);
        assertEquals(2, r.filas().size());
        assertEquals(new BigDecimal("20.00"), r.indicadores().get(0).importe());
        assertEquals(new BigDecimal("10.00"), r.indicadores().get(1).importe());
        assertEquals(new BigDecimal("10.00"), r.indicadores().get(2).importe());
        mvc.perform(get("/reportes").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("2 registro(s)")));
    }

    @Test void canceladasYCuotasHistoricasIndeterminadasNoInflanTotales() {
        var cancelada = viaje(desde, 1, EstadoCuota.ANULADA); cancelada.setEstado(EstadoSalida.CANCELADA); salidas.saveAndFlush(cancelada);
        var antigua = viaje(desde, 2, EstadoCuota.PENDIENTE); antigua.setCuotaAgencia(null); antigua.setEstadoCuota(null); salidas.saveAndFlush(antigua);
        var r = reportes.consultar(ModuloReporte.SALIDAS, desde, hasta);
        assertEquals(2, r.filas().size());
        assertTrue(r.indicadores().stream().allMatch(i -> i.importe().signum() == 0));
    }

    @Test void encomiendasIncluyenUltimoInstanteDelDiaYExcluyenCanceladasDelResumen() {
        paquete(desde.atStartOfDay().minusSeconds(1), EstadoEncomienda.RECIBIDA, EstadoPagoEncomienda.PENDIENTE);
        paquete(desde.atStartOfDay(), EstadoEncomienda.RECIBIDA, EstadoPagoEncomienda.PAGADA);
        paquete(hasta.atTime(23, 59, 59), EstadoEncomienda.RECIBIDA, EstadoPagoEncomienda.PENDIENTE);
        paquete(hasta.atTime(15, 0), EstadoEncomienda.CANCELADA, EstadoPagoEncomienda.PENDIENTE);
        paquete(hasta.plusDays(1).atStartOfDay(), EstadoEncomienda.RECIBIDA, EstadoPagoEncomienda.PENDIENTE);
        var r = reportes.consultar(ModuloReporte.ENCOMIENDAS, desde, hasta);
        assertEquals(3, r.filas().size());
        assertEquals(new BigDecimal("50.00"), r.indicadores().get(0).importe());
        assertEquals(new BigDecimal("25.00"), r.indicadores().get(1).importe());
        assertEquals(new BigDecimal("25.00"), r.indicadores().get(2).importe());
    }

    @Test void cajaCalculaIngresosMenosEgresosSinSumarFondoInicial() throws Exception {
        var caja = cajaService.abrir(new BigDecimal("50.00"));
        movimiento(caja, desde.atStartOfDay().minusSeconds(1), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "999.00");
        movimiento(caja, desde.atStartOfDay(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "30.00");
        movimiento(caja, hasta.atTime(23, 59, 59), TipoMovimientoCaja.INGRESO, MedioPago.YAPE, "70.00");
        movimiento(caja, hasta.atTime(16, 0), TipoMovimientoCaja.EGRESO, MedioPago.PLIN, "40.00");
        movimiento(caja, hasta.plusDays(1).atStartOfDay(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "999.00");
        var r = reportes.consultar(ModuloReporte.CAJA, desde, hasta);
        assertEquals(3, r.filas().size()); assertEquals(new BigDecimal("100.00"), r.indicadores().get(0).importe());
        assertEquals(new BigDecimal("40.00"), r.indicadores().get(1).importe());
        assertEquals(new BigDecimal("60.00"), r.indicadores().get(2).importe());
        mvc.perform(get("/reportes").param("modulo", "CAJA").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Ingresos menos egresos")));
    }

    @Test void descargaExcelConservaSeleccionTextoLiteralYValoresNumericos() throws Exception {
        var caja = cajaService.abrir(BigDecimal.ZERO);
        movimiento(caja, desde.atStartOfDay(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "12.50");
        movimiento(caja, hasta.plusDays(1).atStartOfDay(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "999.00");
        var respuesta = mvc.perform(get("/reportes/exportar").param("modulo", "CAJA")
                        .param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx"))).andReturn();
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(respuesta.getResponse().getContentAsByteArray()))) {
            var hoja = libro.getSheet("Caja"); assertEquals(1, hoja.getLastRowNum());
            assertEquals(CellType.STRING, hoja.getRow(1).getCell(2).getCellType());
            assertEquals("=texto de prueba", hoja.getRow(1).getCell(2).getStringCellValue());
            assertEquals(CellType.NUMERIC, hoja.getRow(1).getCell(4).getCellType());
            assertEquals(12.50, hoja.getRow(1).getCell(4).getNumericCellValue());
        }
    }

    @Test @WithMockUser(roles = "OPERATIVO")
    void operativoConsultaReportesOperativosPeroNoCajaNiIngresosDelInicio() throws Exception {
        assertThrows(AccessDeniedException.class, () -> reportes.consultar(ModuloReporte.CAJA, desde, hasta));
        assertThrows(AccessDeniedException.class, inicio::ingresosHoy);
        mvc.perform(get("/reportes")).andExpect(status().isOk());
        mvc.perform(get("/reportes").param("modulo", "ENCOMIENDAS")).andExpect(status().isOk());
        mvc.perform(get("/reportes").param("modulo", "CAJA")).andExpect(status().isForbidden());
        mvc.perform(get("/reportes/exportar").param("modulo", "CAJA").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("ingresosHoy"))
                .andExpect(content().string(not(containsString("Ingresos de hoy"))));
    }

    @Test @WithMockUser(roles = "CAJA")
    void cajaPuedeConsultarYExportarMovimientos() throws Exception {
        mvc.perform(get("/reportes").param("modulo", "CAJA")).andExpect(status().isOk());
        mvc.perform(get("/reportes/exportar").param("modulo", "CAJA").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk());
    }

    @Test void sinSesionRequiereIngreso() throws Exception {
        mvc.perform(get("/reportes").with(anonymous())).andExpect(status().is3xxRedirection());
    }

    @Test @WithMockUser(roles = "OTRO")
    void rolNoAdmitidoNoPuedeConsultarReportes() throws Exception {
        mvc.perform(get("/reportes")).andExpect(status().isForbidden());
    }

    @Test void periodoInvertidoMuestraErrorYNoEntregaExcel() throws Exception {
        mvc.perform(get("/reportes").param("desde", hasta.toString()).param("hasta", desde.toString()))
                .andExpect(status().isBadRequest()).andExpect(content().string(containsString("Desde no puede ser posterior")));
        mvc.perform(get("/reportes/exportar").param("desde", hasta.toString()).param("hasta", desde.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test void reporteVacioSeMuestraConTotalesEnCero() throws Exception {
        var r = reportes.consultar(ModuloReporte.SALIDAS, desde, hasta);
        assertTrue(r.filas().isEmpty()); assertTrue(r.indicadores().stream().allMatch(i -> i.importe().signum() == 0));
        mvc.perform(get("/reportes")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay registros en este período")));
    }

    @Test void paginacionMantieneLosTotalesYElExcelIncluyeTodoElPeriodo() throws Exception {
        for (int i = 1; i <= 51; i++) viaje(desde, i, EstadoCuota.PENDIENTE);
        var respuesta = mvc.perform(get("/reportes").param("desde", desde.toString()).param("hasta", hasta.toString()).param("pagina", "1"))
                .andExpect(status().isOk()).andExpect(model().attribute("pagina", 1))
                .andExpect(model().attribute("paginas", 2)).andReturn();
        var r = (ReporteService.Resultado) respuesta.getModelAndView().getModel().get("resultado");
        assertEquals(new BigDecimal("510.00"), r.indicadores().get(0).importe());
        assertEquals(1, ((java.util.List<?>) respuesta.getModelAndView().getModel().get("filas")).size());
        var descarga = mvc.perform(get("/reportes/exportar").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isOk()).andReturn();
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(descarga.getResponse().getContentAsByteArray()))) {
            assertEquals(51, libro.getSheet("Salidas").getLastRowNum());
        }
    }

    @Test void indicadoresCuentanActivosYDineroRecibidoHoySinPendientesNiFondoInicial() throws Exception {
        var inactivo = vehiculos.saveAndFlush(Vehiculo.builder().placa("QA-R02").marca("PRUEBA").modelo("INACTIVO")
                .anio(2020).color("BLANCO").capacidadPasajeros(5).numeroTuc("QA-R02").activo(false).build());
        assertFalse(inactivo.getActivo());
        var caja = cajaService.abrir(new BigDecimal("50.00"));
        var hoy = FechaOperativa.hoy();
        movimiento(caja, hoy.atStartOfDay().minusSeconds(1), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "999.00");
        movimiento(caja, hoy.atStartOfDay(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "20.00");
        movimiento(caja, hoy.atTime(23, 59, 59), TipoMovimientoCaja.INGRESO, MedioPago.YAPE, "30.00");
        movimiento(caja, hoy.atTime(16, 0), TipoMovimientoCaja.EGRESO, MedioPago.EFECTIVO, "5.00");
        viaje(hoy, 1, EstadoCuota.PENDIENTE);
        assertEquals(1, inicio.consultar().vehiculosActivos());
        assertEquals(new BigDecimal("50.00"), inicio.ingresosHoy());
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(model().attribute("ingresosHoy", new BigDecimal("50.00")))
                .andExpect(content().string(containsString("Vehículos activos")))
                .andExpect(content().string(containsString("Ingresos de hoy")));
    }

    @Test void propietarioEHistorialConservanRelacionConConductoresAnteriores() throws Exception {
        vehiculo.setPropietario("  Propietario de prueba  "); vehiculoService.guardar(vehiculo);
        viaje(desde, 1, EstadoCuota.PENDIENTE);
        var anterior = conductor;
        conductor = conductores.saveAndFlush(Conductor.builder().dni("00000019").nombres("Segundo")
                .apellidos("Conductor").licencia("TEST-SEGUNDO").build());
        viaje(hasta, 1, EstadoCuota.PAGADA);
        anterior.setActivo(false); conductores.saveAndFlush(anterior);
        var h = vehiculoService.historial(vehiculo.getId(), 0);
        assertEquals(2, h.getTotalElements());
        assertEquals(conductor.getId(), h.getContent().get(0).getConductor().getId());
        assertEquals(anterior.getId(), h.getContent().get(1).getConductor().getId());
        assertEquals("Propietario de prueba", vehiculos.findById(vehiculo.getId()).orElseThrow().getPropietario());
        mvc.perform(get("/vehiculos/" + vehiculo.getId() + "/historial")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Segundo Conductor")))
                .andExpect(content().string(containsString("Prueba Reportes")));
        mvc.perform(get("/vehiculos/999999/historial")).andExpect(status().isNotFound());
        mvc.perform(get("/vehiculos/editar/" + vehiculo.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Propietario de prueba")));
    }

    @Test void formularioGuardaPropietarioYValidaLongitud() throws Exception {
        mvc.perform(post("/vehiculos/guardar").with(csrf()).param("placa", "QA-R03").param("marca", "PRUEBA")
                .param("modelo", "PRUEBA").param("anio", "2020").param("color", "BLANCO").param("capacidadPasajeros", "5")
                .param("numeroTuc", "QA-R03").param("propietario", "Propietario nuevo"))
                .andExpect(status().is3xxRedirection());
        assertTrue(vehiculos.findAll().stream().anyMatch(v -> "Propietario nuevo".equals(v.getPropietario())));
        mvc.perform(post("/vehiculos/guardar").with(csrf()).param("placa", "QA-R04").param("marca", "PRUEBA")
                .param("modelo", "PRUEBA").param("anio", "2020").param("color", "BLANCO").param("capacidadPasajeros", "5")
                .param("numeroTuc", "QA-R04").param("propietario", "A".repeat(121)))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("vehiculo", "propietario"));
        assertFalse(vehiculos.existsByPlaca("QA-R04"));
    }
}
