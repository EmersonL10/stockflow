package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.exception.*;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @WithMockUser(roles="ADMINISTRADOR")
class CajaIntegrationTests {
    @Autowired CajaService caja;
    @Autowired SalidaService salidas;
    @Autowired CajaSesionRepository sesiones;
    @Autowired MovimientoCajaRepository movimientos;
    @Autowired SalidaRepository salidaRepository;
    @Autowired TurnoRepository turnos;
    @Autowired VehiculoRepository vehiculos;
    @Autowired ConductorRepository conductores;
    @Autowired MockMvc mvc;
    @Autowired EncomiendaRepository encomiendas;
    @Autowired EncomiendaEventoRepository eventosEncomienda;
    Turno turno;

    @BeforeEach void preparar() {
        eventosEncomienda.deleteAll(); movimientos.deleteAll(); encomiendas.deleteAll(); sesiones.deleteAll(); salidaRepository.deleteAll(); turnos.deleteAll();
        vehiculos.deleteAll(); conductores.deleteAll();
        var conductor = conductores.saveAndFlush(Conductor.builder().dni("00000009").nombres("Prueba").apellidos("Caja").licencia("TEST-CAJA").build());
        var vehiculo = vehiculos.saveAndFlush(Vehiculo.builder().placa("QA-C01").marca("PRUEBA").modelo("CAJA").anio(2020)
                .color("BLANCO").capacidadPasajeros(5).numeroTuc("TEST-CAJA").build());
        turno = turnos.saveAndFlush(Turno.builder().fecha(LocalDate.now(ZoneId.of("America/Lima"))).numeroTurno(1)
                .sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).horaProgramada(LocalTime.NOON).vehiculo(vehiculo).conductor(conductor).build());
    }
    BigDecimal soles(String valor) { return new BigDecimal(valor); }
    Salida salida(boolean pagada, MedioPago medio) {
        return Salida.builder().turno(Turno.builder().id(turno.getId()).build()).horaSalidaReal(LocalTime.NOON)
                .cantidadPasajeros(3).pagoAlSalir(pagada).medioPago(medio).build();
    }
    void manual(Long id, TipoMovimientoCaja tipo, MedioPago medio, String importe) {
        caja.registrarManual(id, tipo, medio, soles(importe), "Concepto de prueba", UUID.randomUUID().toString());
    }

    @Test void resumenDistingueNetoDeMovimientosFondoInicialYEfectivoFisico() throws Exception {
        var c = caja.abrir(soles("10.00"));
        manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "30.00");
        manual(c.getId(), TipoMovimientoCaja.EGRESO, MedioPago.EFECTIVO, "30.00");
        manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.YAPE, "70.00");
        manual(c.getId(), TipoMovimientoCaja.EGRESO, MedioPago.PLIN, "10.00");
        var resumen = caja.resumir(c, caja.movimientos(c.getId()));
        assertEquals(soles("100.00"), resumen.ingresos()); assertEquals(soles("40.00"), resumen.egresos());
        assertEquals(soles("70.00"), resumen.totalRegistrado());
        assertEquals(soles("60.00"), resumen.neto()); assertEquals(soles("60.00"), resumen.netoDigital());
        assertEquals(soles("10.00"), resumen.efectivoEsperado());
        mvc.perform(get("/caja/" + c.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Saldo neto de movimientos")));
    }

    @Test void cierreFallidoConservaConteoYPermiteCorregirMotivo() throws Exception {
        var c = caja.abrir(soles("10.00"));
        var intento = mvc.perform(post("/caja/" + c.getId() + "/cerrar").with(csrf()).param("contado","9.00").param("observacion",""))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("mostrarCierre", true))
                .andExpect(flash().attribute("contadoCierre", soles("9.00")))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("Faltan S/ 1.00"))).andReturn();
        assertTrue(sesiones.findById(c.getId()).orElseThrow().isAbierta());
        mvc.perform(get("/caja/" + c.getId()).flashAttrs(intento.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"9.00\"")));
        mvc.perform(post("/caja/" + c.getId() + "/cerrar").with(csrf()).param("contado","9.00").param("observacion","Faltante confirmado al contar"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("mensaje"));
        var cerrada = sesiones.findById(c.getId()).orElseThrow();
        assertFalse(cerrada.isAbierta()); assertEquals(soles("9.00"), cerrada.getEfectivoContado());
        assertEquals(soles("-1.00"), cerrada.getDiferencia());
    }

    @Test void corteDiarioMuestraArqueoPendienteYPermiteCerrarAyerYAbrirHoy() throws Exception {
        var c = caja.abrir(soles("10.00"));
        c.setApertura(c.getApertura().minusDays(1)); sesiones.saveAndFlush(c);
        assertTrue(c.isPendienteArqueo()); assertFalse(c.isOperativaHoy());
        mvc.perform(get("/caja/" + c.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pendiente de arqueo")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-movimientos-diarios"))));
        mvc.perform(post("/caja/" + c.getId() + "/movimientos").with(csrf()).param("tipo","INGRESO").param("medio","YAPE")
                .param("importe","1.00").param("concepto","No debe aceptar").param("solicitud",UUID.randomUUID().toString()))
                .andExpect(flash().attribute("error", org.hamcrest.Matchers.containsString("pendiente de arqueo")));
        assertEquals(0, movimientos.count());
        mvc.perform(post("/caja/" + c.getId() + "/cerrar").with(csrf()).param("contado","10.00"))
                .andExpect(flash().attributeExists("mensaje"));
        mvc.perform(post("/caja/abrir").with(csrf()).param("saldoInicial","10.00"))
                .andExpect(flash().attributeExists("mensaje"));
        assertTrue(sesiones.findByClaveActiva(1).orElseThrow().isOperativaHoy());
        assertFalse(sesiones.findById(c.getId()).orElseThrow().isAbierta());
    }

    @Test void aperturaMovimientosDigitalesYArqueoConDiferencia() {
        var c = caja.abrir(soles("50.00"));
        manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "20.00");
        manual(c.getId(), TipoMovimientoCaja.EGRESO, MedioPago.EFECTIVO, "5.00");
        manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.YAPE, "30.00");
        manual(c.getId(), TipoMovimientoCaja.EGRESO, MedioPago.TRANSFERENCIA, "12.00");
        var resumen = caja.resumir(c, caja.movimientos(c.getId()));
        assertEquals(soles("65.00"), resumen.efectivoEsperado());
        assertEquals(soles("30.00"), resumen.ingresosDigitales());
        assertEquals(soles("12.00"), resumen.egresosDigitales());
        assertThrows(CajaOperacionException.class, () -> caja.cerrar(c.getId(), soles("64.00"), ""));
        caja.cerrar(c.getId(), soles("64.00"), "Faltante identificado durante el arqueo");
        var cerrada = caja.buscar(c.getId());
        assertEquals(soles("-1.00"), cerrada.getDiferencia());
        assertEquals(soles("65.00"), cerrada.getEfectivoEsperado());
        assertFalse(cerrada.isAbierta()); assertNotNull(cerrada.getCerradaPor());
        assertThrows(CajaOperacionException.class, () -> manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "1.00"));
        assertThrows(CajaOperacionException.class, () -> caja.cerrar(c.getId(), soles("64.00"), "Segundo cierre"));
        assertNotNull(caja.abrir(soles("64.00")).getId());
    }

    @Test void pagoSinCajaRevierteElCobroYAltaPagadaCompleta() {
        assertThrows(CajaOperacionException.class, () -> salidas.guardar(salida(true, MedioPago.EFECTIVO)));
        assertEquals(0, salidaRepository.count());
        assertEquals(EstadoTurno.PROGRAMADO, turnos.findById(turno.getId()).orElseThrow().getEstado());
        var s = salidas.guardar(salida(false, MedioPago.EFECTIVO));
        assertThrows(CajaOperacionException.class, () -> salidas.registrarPago(s.getId()));
        assertEquals(EstadoCuota.PENDIENTE, salidaRepository.findById(s.getId()).orElseThrow().getEstadoCuota());
        assertEquals(0, movimientos.count());
    }

    @Test void cobraAlSalirUnaVezYNoDuplicaAlEditar() {
        var c = caja.abrir(soles("0.00"));
        var s = salidas.guardar(salida(true, MedioPago.YAPE));
        var editar = salida(false, MedioPago.EFECTIVO); editar.setId(s.getId()); editar.setEstado(EstadoSalida.FINALIZADA);
        salidas.guardar(editar);
        assertEquals(1, movimientos.count());
        var m = caja.movimientos(c.getId()).getFirst();
        assertEquals(s.getId(), m.getSalida().getId()); assertEquals(MedioPago.YAPE, m.getMedio());
        assertEquals(soles("10.00"), m.getImporte());
        assertEquals(soles("0.00"), caja.resumir(c, caja.movimientos(c.getId())).efectivoEsperado());
        assertThrows(SalidaValidationException.class, () -> salidas.registrarPago(s.getId()));
    }

    @Test void altaPagadaSinCajaDevuelveFormularioReintentableSinIdFantasma() throws Exception {
        mvc.perform(post("/salidas/guardar").with(csrf()).param("turno.id", turno.getId().toString())
                .param("horaSalidaReal", "12:00").param("cantidadPasajeros", "3")
                .param("estado", "EN_RUTA").param("pagoAlSalir", "true").param("medioPago", "EFECTIVO"))
                .andExpect(status().isOk()).andExpect(view().name("salidas/formulario"))
                .andExpect(model().attribute("salida", org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.nullValue())));
        assertEquals(0, salidaRepository.count());
    }

    @Test void cuotaAnteriorSeCobraEnLaCajaActualYElHistoricoNoSeImporta() {
        var pendiente = salidas.guardar(salida(false, MedioPago.EFECTIVO));
        // Simula un viaje histórico ya registrado; no crea una salida retroactiva.
        turno.setFecha(turno.getFecha().minusDays(2)); turnos.saveAndFlush(turno);
        var c = caja.abrir(soles("20.00"));
        salidas.registrarPago(pendiente.getId(), MedioPago.EFECTIVO);
        assertEquals(soles("30.00"), caja.resumir(c, caja.movimientos(c.getId())).efectivoEsperado());
        assertEquals(0, caja.pagosSinCaja());
        movimientos.deleteAll(); // Simula un cobro anterior a la implementación de Caja, solo en la base de pruebas.
        assertEquals(1, caja.pagosSinCaja());
        assertEquals(soles("20.00"), caja.resumir(c, caja.movimientos(c.getId())).efectivoEsperado());
    }

    @Test void rechazaDuplicadosImportesInvalidosYSobregiro() {
        var c = caja.abrir(soles("5.00"));
        assertThrows(CajaOperacionException.class, () -> caja.abrir(soles("0.00")));
        assertThrows(CajaOperacionException.class, () -> manual(c.getId(), TipoMovimientoCaja.EGRESO, MedioPago.EFECTIVO, "5.01"));
        assertThrows(CajaOperacionException.class, () -> manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "0"));
        assertThrows(CajaOperacionException.class, () -> manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "-1"));
        assertThrows(CajaOperacionException.class, () -> manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "1.001"));
        var token = UUID.randomUUID().toString();
        caja.registrarManual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, soles("1.00"), "Prueba", token);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> caja.registrarManual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, soles("1.00"), "Prueba", token));
        assertEquals(1, movimientos.count());
    }

    @Test void cajaDeOtroDiaSoloAdmiteCierre() {
        var c = caja.abrir(soles("0.00"));
        c.setApertura(c.getApertura().minusDays(1)); sesiones.saveAndFlush(c);
        assertThrows(CajaOperacionException.class, () -> manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.YAPE, "1.00"));
        var s = salidas.guardar(salida(false, MedioPago.EFECTIVO));
        assertThrows(CajaOperacionException.class, () -> salidas.registrarPago(s.getId()));
        caja.cerrar(c.getId(), soles("0.00"), "Cierre de caja anterior");
        assertTrue(caja.abierta().isEmpty());
    }

    @Test void seguridadCsrfFormulariosYVistaDeCierre() throws Exception {
        mvc.perform(get("/caja").with(user("operador").roles("OPERATIVO"))).andExpect(status().isForbidden());
        mvc.perform(post("/caja/abrir").with(user("operador").roles("OPERATIVO")).with(csrf()).param("saldoInicial","0.00")).andExpect(status().isForbidden());
        mvc.perform(post("/caja/abrir").param("saldoInicial","0.00")).andExpect(status().isForbidden());
        mvc.perform(get("/caja").with(user("cajero").roles("CAJA"))).andExpect(status().isOk());
        mvc.perform(post("/caja/abrir").with(csrf()).param("saldoInicial", "-1")).andExpect(status().is3xxRedirection());
        assertEquals(0, sesiones.count());
        mvc.perform(post("/caja/abrir").with(csrf()).param("saldoInicial", "20.00").param("claveActiva", "0"))
                .andExpect(status().is3xxRedirection());
        var c = sesiones.findByClaveActiva(1).orElseThrow();
        mvc.perform(get("/caja/" + c.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Efectivo esperado")));
        var token = UUID.randomUUID().toString();
        mvc.perform(post("/caja/" + c.getId() + "/movimientos").with(csrf()).param("tipo","EGRESO").param("medio","EFECTIVO")
                .param("importe","5.00").param("concepto","Útiles").param("solicitud",token).param("registradoPor","otro"))
                .andExpect(status().is3xxRedirection());
        assertEquals("user", movimientos.findAll().getFirst().getRegistradoPor());
        mvc.perform(post("/caja/" + c.getId() + "/cerrar").with(csrf()).param("contado","15.00").param("observacion", "Cierre correcto")
                .param("efectivoEsperado","0.00").param("diferencia","999.00"))
                .andExpect(status().is3xxRedirection());
        assertEquals(soles("0.00"), sesiones.findById(c.getId()).orElseThrow().getDiferencia());
        mvc.perform(get("/caja/" + c.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Efectivo confirmado al cerrar")));
        mvc.perform(get("/caja/999999")).andExpect(status().is3xxRedirection());
    }

    @Test void soloUnaAperturaSimultanea() throws Exception {
        assertEquals(1, paralelos(() -> caja.abrir(soles("0.00")), () -> caja.abrir(soles("0.00"))));
        assertEquals(1, sesiones.count());
    }

    @Test void cierreAdmiteSaldoAcumuladoMayorQueUnSoloMovimiento() {
        var c = caja.abrir(soles("99999999.99"));
        manual(c.getId(), TipoMovimientoCaja.INGRESO, MedioPago.EFECTIVO, "10.00");
        caja.cerrar(c.getId(), soles("100000009.99"), "");
        assertEquals(soles("0.00"), caja.buscar(c.getId()).getDiferencia());
    }

    @Test void doblePagoCreaSoloUnIngreso() throws Exception {
        caja.abrir(soles("0.00"));
        var s = salidas.guardar(salida(false, MedioPago.EFECTIVO));
        assertEquals(1, paralelos(() -> { salidas.registrarPago(s.getId()); return null; },
                () -> { salidas.registrarPago(s.getId()); return null; }));
        assertEquals(1, movimientos.count());
    }

    @Test void cierreConcurrenteConPagoNoPierdeNiDesvinculaDinero() throws Exception {
        var c = caja.abrir(soles("0.00"));
        var s = salidas.guardar(salida(false, MedioPago.EFECTIVO));
        paralelos(() -> { caja.cerrar(c.getId(), soles("0.00"), "Arqueo de prueba concurrente"); return null; },
                () -> { salidas.registrarPago(s.getId()); return null; });
        var cerrada = caja.buscar(c.getId());
        assertFalse(cerrada.isAbierta());
        boolean pagada = salidaRepository.findById(s.getId()).orElseThrow().getEstadoCuota() == EstadoCuota.PAGADA;
        assertEquals(pagada ? 1 : 0, movimientos.count());
        assertEquals(pagada ? soles("10.00") : soles("0.00"), cerrada.getEfectivoEsperado());
    }

    int paralelos(Callable<?> primero, Callable<?> segundo) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (var accion : List.of(primero, segundo)) resultados.add(pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        "pruebas", "", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
                inicio.await();
                try { accion.call(); return true; } catch (RuntimeException ex) { return false; }
                finally { SecurityContextHolder.clearContext(); }
            }));
            inicio.countDown(); int exitos = 0;
            for (var r : resultados) if (r.get(20, TimeUnit.SECONDS)) exitos++;
            return exitos;
        }
    }
}
