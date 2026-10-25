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
import pe.com.honoriaexpress.stockflow.dto.*;
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
class EncomiendasIntegrationTests {
    @Autowired EncomiendaService service;
    @Autowired EncomiendaRepository encomiendas;
    @Autowired EncomiendaEventoRepository eventos;
    @Autowired CajaService caja;
    @Autowired CajaSesionRepository sesiones;
    @Autowired MovimientoCajaRepository movimientos;
    @Autowired SalidaService salidaService;
    @Autowired SalidaRepository salidas;
    @Autowired TurnoRepository turnos;
    @Autowired VehiculoRepository vehiculos;
    @Autowired ConductorRepository conductores;
    @Autowired MockMvc mvc;
    @Autowired TurnoService turnoService;
    Salida salida;

    @BeforeEach void preparar() {
        eventos.deleteAll(); movimientos.deleteAll(); encomiendas.deleteAll(); sesiones.deleteAll(); salidas.deleteAll();
        turnos.deleteAll(); vehiculos.deleteAll(); conductores.deleteAll();
        var c = conductores.saveAndFlush(Conductor.builder().dni("00000008").nombres("Prueba").apellidos("Encomiendas").licencia("TEST-ENC").build());
        var v = vehiculos.saveAndFlush(Vehiculo.builder().placa("QA-E01").marca("PRUEBA").modelo("ENVIO").anio(2020)
                .color("BLANCO").capacidadPasajeros(5).numeroTuc("QA-ENC").build());
        var turno = turnos.saveAndFlush(Turno.builder().fecha(LocalDate.now(ZoneId.of("America/Lima"))).numeroTurno(1)
                .sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).horaProgramada(LocalTime.NOON).vehiculo(v).conductor(c).build());
        salida = salidaService.guardar(Salida.builder().turno(turno).horaSalidaReal(LocalTime.NOON).cantidadPasajeros(4).build());
    }
    EncomiendaFormulario datos() {
        var f = new EncomiendaFormulario();
        f.setRemitente("Remitente de prueba"); f.setDocumentoRemitente("00000001"); f.setTelefonoRemitente("900000001");
        f.setDestinatario("Destinatario de prueba"); f.setDocumentoDestinatario("00000002"); f.setTelefonoDestinatario("900000002");
        f.setRuta("Pucallpa - Honoria"); f.setDescripcion("Contenido de prueba"); f.setBultos(2); f.setPesoKg(new BigDecimal("1.250"));
        f.setImporte(new BigDecimal("25.00")); f.setSolicitud(UUID.randomUUID().toString()); return f;
    }
    Encomienda nueva() { return service.registrar(datos()); }

    Turno espera() {
        var original = salida.getTurno();
        return turnos.saveAndFlush(Turno.builder().fecha(FechaOperativa.hoy()).numeroTurno(100 + (int)turnos.count())
                .sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).horaProgramada(LocalTime.of(14, 0))
                .vehiculo(original.getVehiculo()).conductor(original.getConductor()).build());
    }
    void cargarYSalir(Encomienda e) {
        var turno = espera(); service.asignar(e.getId(), turno.getId());
        salida = salidaService.guardar(Salida.builder().turno(turno).horaSalidaReal(LocalTime.of(14, 0)).cantidadPasajeros(4).cargaConfirmada(true).cargaIds(e.getId().toString()).build());
    }

    @Test void flujoCompletoConPagoDigitalEntregaYSeguimiento() throws Exception {
        var e = nueva();
        assertTrue(e.getCodigo().matches("ENC-\\d{8,}"));
        assertEquals(EstadoEncomienda.RECIBIDA, e.getEstado());
        assertEquals(EstadoPagoEncomienda.PENDIENTE, e.getEstadoPago());
        var c = caja.abrir(BigDecimal.ZERO);
        service.cobrar(e.getId(), MedioPago.YAPE);
        cargarYSalir(e); service.recibirDestino(e.getId());
        service.entregar(e.getId(), new EntregaEncomiendaFormulario("Destinatario de prueba", "00000002"));
        var entregada = service.buscar(e.getId());
        assertEquals(EstadoEncomienda.ENTREGADA, entregada.getEstado());
        assertNotNull(entregada.getEntregadaEn()); assertEquals("00000002", entregada.getDocumentoReceptor());
        assertEquals(6, service.eventos(e.getId()).size());
        assertEquals(1, movimientos.count());
        var m = movimientos.findAll().getFirst(); assertEquals(e.getId(), m.getEncomienda().getId()); assertNull(m.getSalida());
        assertEquals(new BigDecimal("25.00"), caja.resumir(c, caja.movimientos(c.getId())).ingresosDigitales());
        assertEquals(new BigDecimal("0.00"), caja.resumir(c, caja.movimientos(c.getId())).efectivoEsperado());
        mvc.perform(get("/encomiendas/" + e.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Historial de seguimiento")));
        mvc.perform(get("/caja/" + c.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(e.getCodigo())));
    }

    @Test void cobroSinCajaReviertePagoYEvento() {
        var e = nueva();
        assertThrows(CajaOperacionException.class, () -> service.cobrar(e.getId(), MedioPago.EFECTIVO));
        var guardada = service.buscar(e.getId());
        assertEquals(EstadoPagoEncomienda.PENDIENTE, guardada.getEstadoPago()); assertNull(guardada.getFechaPago());
        assertEquals(1, service.eventos(e.getId()).size()); assertEquals(0, movimientos.count());
    }

    @Test void validaImportesBultosTelefonosYToken() {
        var f = datos(); f.setImporte(new BigDecimal("0"));
        assertThrows(EncomiendaOperacionException.class, () -> service.registrar(f));
        f.setImporte(new BigDecimal("1.001")); assertThrows(EncomiendaOperacionException.class, () -> service.registrar(f));
        f.setImporte(new BigDecimal("1.00")); f.setBultos(0); assertThrows(EncomiendaOperacionException.class, () -> service.registrar(f));
        f.setBultos(1); f.setTelefonoRemitente("-------"); assertThrows(EncomiendaOperacionException.class, () -> service.registrar(f));
        f.setTelefonoRemitente("900000001"); f.setSolicitud(null); assertThrows(EncomiendaOperacionException.class, () -> service.registrar(f));
        assertEquals(0, encomiendas.count());
    }

    @Test void edicionSoloAntesDePagoYDespacho() {
        var e = nueva(); var cambios = datos(); cambios.setImporte(new BigDecimal("30.00"));
        service.editar(e.getId(), cambios);
        assertEquals(e.getCodigo(), service.buscar(e.getId()).getCodigo());
        assertEquals(new BigDecimal("30.00"), service.buscar(e.getId()).getImporte());
        caja.abrir(BigDecimal.ZERO); service.cobrar(e.getId(), MedioPago.EFECTIVO);
        assertThrows(EncomiendaOperacionException.class, () -> service.editar(e.getId(), datos()));
        assertEquals(new BigDecimal("30.00"), movimientos.findAll().getFirst().getImporte());
        assertThrows(EncomiendaOperacionException.class, () -> service.cancelar(e.getId(), "No debe cancelar un cobro"));
    }

    @Test void cancelacionConservaRegistroYAnulaPagoPendiente() {
        var e = nueva(); assertThrows(EncomiendaOperacionException.class, () -> service.cancelar(e.getId(), " "));
        service.cancelar(e.getId(), "Cliente desiste antes del despacho");
        var cancelada = service.buscar(e.getId());
        assertEquals(EstadoEncomienda.CANCELADA, cancelada.getEstado()); assertEquals(EstadoPagoEncomienda.ANULADA, cancelada.getEstadoPago());
        assertThrows(EncomiendaOperacionException.class, () -> service.cobrar(e.getId(), MedioPago.EFECTIVO));
        assertThrows(EncomiendaOperacionException.class, () -> service.despachar(e.getId(), salida.getId()));
        assertEquals(2, service.eventos(e.getId()).size()); assertEquals(0, movimientos.count());
    }

    @Test void despachoVerificaRutaFechaYEstadoDeSalida() {
        var e = nueva();
        var distinta = datos(); distinta.setRuta("Honoria - Pucallpa"); var otro = service.registrar(distinta);
        assertThrows(EncomiendaOperacionException.class, () -> service.despachar(otro.getId(), salida.getId()));
        assertEquals(1, service.salidasDisponibles(e.getRuta()).size());
        salida.setEstado(EstadoSalida.FINALIZADA); salidas.saveAndFlush(salida);
        assertThrows(EncomiendaOperacionException.class, () -> service.despachar(e.getId(), salida.getId()));
        salida.setEstado(EstadoSalida.EN_RUTA); salidas.saveAndFlush(salida);
        var t = salida.getTurno(); t.setFecha(t.getFecha().minusDays(1)); turnos.saveAndFlush(t);
        assertThrows(EncomiendaOperacionException.class, () -> service.despachar(e.getId(), salida.getId()));
        assertTrue(service.salidasDisponibles(e.getRuta()).isEmpty());
    }

    @Test void noCancelaSalidaQueTransportaEncomiendaNiEditaEnvioEnTransito() {
        var e = nueva(); cargarYSalir(e);
        var cancelar = Salida.builder().id(salida.getId()).turno(salida.getTurno()).estado(EstadoSalida.CANCELADA)
                .horaSalidaReal(LocalTime.NOON).cantidadPasajeros(4).build();
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(cancelar));
        assertEquals(EstadoSalida.EN_RUTA, salidas.findById(salida.getId()).orElseThrow().getEstado());
        assertThrows(EncomiendaOperacionException.class, () -> service.editar(e.getId(), datos()));
        assertThrows(EncomiendaOperacionException.class, () -> service.cancelar(e.getId(), "No debe cancelar"));
    }

    @Test void entregaRequiereDestinoPagoYDocumentoCorrecto() {
        var e = nueva(); var receptor = new EntregaEncomiendaFormulario("Destinatario", "00000002");
        assertThrows(EncomiendaOperacionException.class, () -> service.entregar(e.getId(), receptor));
        assertThrows(EncomiendaOperacionException.class, () -> service.recibirDestino(e.getId()));
        cargarYSalir(e); service.recibirDestino(e.getId());
        assertThrows(EncomiendaOperacionException.class, () -> service.entregar(e.getId(), receptor));
        caja.abrir(BigDecimal.ZERO); service.cobrar(e.getId(), MedioPago.EFECTIVO);
        assertThrows(EncomiendaOperacionException.class, () -> service.entregar(e.getId(), new EntregaEncomiendaFormulario("Otra persona", "00000999")));
        assertEquals(EstadoEncomienda.DISPONIBLE, service.buscar(e.getId()).getEstado());
        service.entregar(e.getId(), receptor);
        assertThrows(EncomiendaOperacionException.class, () -> service.entregar(e.getId(), receptor));
    }

    @Test void consultaPaginadaYFiltrosNoInterpretanComodines() {
        for (int i = 0; i < 21; i++) nueva();
        var pagina = service.consultar("", EstadoEncomienda.RECIBIDA, EstadoPagoEncomienda.PENDIENTE, 0);
        assertEquals(21, pagina.getTotalElements()); assertEquals(20, pagina.getNumberOfElements()); assertTrue(pagina.hasNext());
        assertEquals(1, service.consultar("", null, null, 1).getNumberOfElements());
        assertTrue(service.consultar("%", null, null, 0).isEmpty());
        assertEquals(1, service.consultar(pagina.getContent().getFirst().getCodigo().toLowerCase(), null, null, 0).getTotalElements());
        assertTrue(service.consultar("", EstadoEncomienda.ENTREGADA, null, 0).isEmpty());
    }

    @Test void formularioHttpProtegeEstadosImportesDeCobroYDatosDeAuditoria() throws Exception {
        var r = mvc.perform(post("/encomiendas/guardar").with(csrf()).param("remitente","Prueba remitente")
                .param("documentoRemitente","00000001").param("telefonoRemitente","900000001")
                .param("destinatario","Prueba destinatario").param("documentoDestinatario","00000002").param("telefonoDestinatario","900000002")
                .param("ruta","Pucallpa - Honoria").param("descripcion","Paquete de prueba").param("bultos","1").param("importe","25.00")
                .param("solicitud",UUID.randomUUID().toString()).param("estado","ENTREGADA").param("estadoPago","PAGADA")
                .param("codigo","FALSO").param("pagoRegistradoPor","otro"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var e = encomiendas.findAll().getFirst();
        assertEquals(EstadoEncomienda.RECIBIDA, e.getEstado()); assertEquals(EstadoPagoEncomienda.PENDIENTE, e.getEstadoPago());
        assertNotEquals("FALSO", e.getCodigo()); assertNull(e.getPagoRegistradoPor()); assertEquals(0, movimientos.count());
        mvc.perform(get(r.getResponse().getRedirectedUrl())).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/encomiendas/" + e.getId() + "/editar")).andExpect(status().isOk());
        mvc.perform(get("/encomiendas").param("codigo",e.getCodigo())).andExpect(status().isOk());
        mvc.perform(get("/encomiendas/nuevo")).andExpect(status().isOk());
    }

    @Test void seguridadCsrfYPermisosPorOperacion() throws Exception {
        var e = nueva();
        mvc.perform(get("/encomiendas").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/encomiendas").with(user("invitado").roles("INVITADO"))).andExpect(status().isForbidden());
        mvc.perform(post("/encomiendas/guardar")).andExpect(status().isForbidden());
        mvc.perform(post("/encomiendas/" + e.getId() + "/pago").with(csrf()).with(user("operador").roles("OPERATIVO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/encomiendas/nuevo").with(user("cajero").roles("CAJA"))).andExpect(status().isForbidden());
        mvc.perform(post("/encomiendas/" + e.getId() + "/despachar").with(csrf()).with(user("cajero").roles("CAJA")).param("salidaId",salida.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/encomiendas/" + e.getId()).with(user("cajero").roles("CAJA"))).andExpect(status().isOk());
        mvc.perform(get("/encomiendas/999999")).andExpect(status().is3xxRedirection());
    }

    @Test void reenvioConcurrenteSoloCreaUnRegistro() throws Exception {
        var f = datos();
        assertEquals(1, paralelos(() -> service.registrar(f), () -> service.registrar(f)));
        assertEquals(1, encomiendas.count()); assertEquals(1, eventos.count());
    }

    @Test void cobrosConcurrentesSoloGeneranUnIngreso() throws Exception {
        var e = nueva(); caja.abrir(BigDecimal.ZERO);
        assertEquals(1, paralelos(() -> { service.cobrar(e.getId(), MedioPago.EFECTIVO); return null; },
                () -> { service.cobrar(e.getId(), MedioPago.YAPE); return null; }));
        assertEquals(1, movimientos.count()); assertEquals(2, eventos.count());
    }

    @Test void cierreConcurrenteNoDejaPagoDesvinculado() throws Exception {
        var e = nueva(); var c = caja.abrir(BigDecimal.ZERO);
        paralelos(() -> { service.cobrar(e.getId(), MedioPago.EFECTIVO); return null; },
                () -> { caja.cerrar(c.getId(), BigDecimal.ZERO, "Arqueo concurrente de prueba"); return null; });
        assertFalse(caja.buscar(c.getId()).isAbierta());
        boolean pagada = service.buscar(e.getId()).getEstadoPago() == EstadoPagoEncomienda.PAGADA;
        assertEquals(pagada ? 1 : 0, movimientos.count());
        assertEquals(new BigDecimal(pagada ? "25.00" : "0.00"), caja.buscar(c.getId()).getEfectivoEsperado());
    }

    @Test void asignacionConcurrenteConSalidaNoAgregaPaquetesDespuesDeSalir() throws Exception {
        var e = nueva(); var t = espera();
        var viaje = Salida.builder().turno(t).horaSalidaReal(LocalTime.NOON).cantidadPasajeros(1).build();
        assertEquals(1, paralelos(() -> { service.asignar(e.getId(), t.getId()); return null; }, () -> salidaService.guardar(viaje)));
        var estado = service.buscar(e.getId()).getEstado();
        assertTrue(estado == EstadoEncomienda.ASIGNADA || estado == EstadoEncomienda.RECIBIDA);
        assertEquals(estado == EstadoEncomienda.ASIGNADA ? 0 : 1, salidas.existsByTurnoId(t.getId()) ? 1 : 0);
    }

    @Test void asignacionValidaRutaFechaYPermiteRetirarAntesDeSalir() throws Exception {
        var e = nueva(); var t = espera();
        var otra = datos(); otra.setRuta("Honoria - Pucallpa"); var contrario = service.registrar(otra);
        assertThrows(EncomiendaOperacionException.class, () -> service.asignar(contrario.getId(), t.getId()));
        t.setFecha(FechaOperativa.hoy().minusDays(1)); turnos.saveAndFlush(t);
        assertThrows(EncomiendaOperacionException.class, () -> service.asignar(e.getId(), t.getId()));
        t.setFecha(FechaOperativa.hoy()); turnos.saveAndFlush(t);
        service.asignar(e.getId(), t.getId());
        assertEquals(EstadoEncomienda.ASIGNADA, service.buscar(e.getId()).getEstado());
        mvc.perform(get("/encomiendas/" + e.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quién lleva la encomienda")));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("prueba", "", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
        service.retirarAsignacion(e.getId());
        assertEquals(EstadoEncomienda.RECIBIDA, service.buscar(e.getId()).getEstado());
        assertNull(service.buscar(e.getId()).getTurnoAsignado());
    }

    @Test void salidaExigeCargaConfirmadaYDespachaTodosLosPaquetesAtomicamente() throws Exception {
        var e = nueva(); var otro = nueva(); var t = espera();
        service.asignar(e.getId(), t.getId()); service.asignar(otro.getId(), t.getId());
        var viaje = Salida.builder().turno(t).horaSalidaReal(LocalTime.NOON).cantidadPasajeros(1).build();
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(viaje));
        assertFalse(salidas.existsByTurnoId(t.getId()));
        viaje.setCargaConfirmada(true); viaje.setCargaIds(e.getId() + "," + otro.getId()); viaje.setRutaReal("Honoria - Pucallpa");
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(viaje));
        viaje.setRutaReal("Pucallpa-Honoria"); var registrada = salidaService.guardar(viaje);
        for (var paquete : List.of(e, otro)) {
            var guardado = service.buscar(paquete.getId());
            assertEquals(EstadoEncomienda.EN_TRANSITO, guardado.getEstado());
            assertEquals(registrada.getId(), guardado.getSalida().getId());
            assertEquals(EstadoPagoEncomienda.PENDIENTE, guardado.getEstadoPago());
            assertThrows(EncomiendaOperacionException.class, () -> service.retirarAsignacion(paquete.getId()));
        }
    }

    @Test void colaDiariaNoMezclaPendientesAntiguosNiReescribeSuFecha() throws Exception {
        var antiguo = espera(); antiguo.setFecha(FechaOperativa.hoy().minusDays(1)); turnos.saveAndFlush(antiguo);
        var actual = espera();
        assertEquals(List.of(actual.getId()), turnoService.listarColaPendiente().stream().map(Turno::getId).toList());
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(Salida.builder().turno(antiguo).horaSalidaReal(LocalTime.NOON).cantidadPasajeros(1).build()));
        assertThrows(SalidaValidationException.class, () -> turnoService.guardar(antiguo));
        var cambiar = Turno.builder().id(antiguo.getId()).fecha(FechaOperativa.hoy()).numeroTurno(antiguo.getNumeroTurno())
                .sentidoRuta(antiguo.getSentidoRuta()).horaProgramada(LocalTime.NOON).vehiculo(antiguo.getVehiculo()).conductor(antiguo.getConductor()).build();
        assertThrows(SalidaValidationException.class, () -> turnoService.guardar(cambiar));
        assertEquals(FechaOperativa.hoy().minusDays(1), turnos.findById(antiguo.getId()).orElseThrow().getFecha());
        mvc.perform(get("/turnos/nuevo")).andExpect(status().isOk())
                .andExpect(model().attribute("turno", org.hamcrest.Matchers.hasProperty("fecha", org.hamcrest.Matchers.is(FechaOperativa.hoy()))));
    }

    @Test void viajesDeHoyPorFechaYTodosNoOcultanElHistorial() throws Exception {
        var t = salida.getTurno(); t.setFecha(FechaOperativa.hoy().minusDays(2)); turnos.saveAndFlush(t);
        mvc.perform(get("/salidas")).andExpect(status().isOk())
                .andExpect(model().attribute("salidas", org.hamcrest.Matchers.empty()))
                .andExpect(model().attribute("totalHistorico", 1))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ver todos los viajes")));
        mvc.perform(get("/salidas").param("todasFechas", "true")).andExpect(status().isOk())
                .andExpect(model().attribute("salidas", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/salidas").param("fecha", t.getFecha().toString())).andExpect(status().isOk())
                .andExpect(model().attribute("salidas", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test void cambioDeCargaDesdeQueSeAbreFormularioExigeNuevaRevision() throws Exception {
        var t = espera(); var primero = nueva(); service.asignar(primero.getId(), t.getId());
        mvc.perform(get("/salidas/nuevo").param("turnoId", t.getId().toString())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(primero.getCodigo())));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("prueba", "", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
        var segundo = nueva(); service.asignar(segundo.getId(), t.getId());
        var viaje = Salida.builder().turno(t).horaSalidaReal(LocalTime.NOON).cantidadPasajeros(1).cargaConfirmada(true).cargaIds(primero.getId().toString()).build();
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(viaje));
        assertFalse(salidas.existsByTurnoId(t.getId()));
        assertEquals(EstadoEncomienda.ASIGNADA, service.buscar(segundo.getId()).getEstado());
    }

    @Test void rutaLibreAsignaAntesDeSalirYCobroFallidoRevierteDespacho() {
        var t = espera(); t.setRutaOperativa("Pucallpa - Aguaytía"); turnos.saveAndFlush(t);
        var f = datos(); f.setRuta("Pucallpa - Aguaytía"); var e = service.registrar(f);
        assertEquals(1, service.turnosDisponibles(f.getRuta()).size()); service.asignar(e.getId(), t.getId());
        var viaje = Salida.builder().turno(t).horaSalidaReal(LocalTime.NOON).cantidadPasajeros(1)
                .cargaConfirmada(true).cargaIds(e.getId().toString()).pagoAlSalir(true).build();
        assertThrows(CajaOperacionException.class, () -> salidaService.guardar(viaje));
        assertFalse(salidas.existsByTurnoId(t.getId()));
        assertEquals(EstadoEncomienda.ASIGNADA, service.buscar(e.getId()).getEstado());
        assertNull(service.buscar(e.getId()).getSalida());
        assertEquals(2, service.eventos(e.getId()).size());
    }

    @Test void asignarTransporteExigeRolOperativoYProteccionCsrf() throws Exception {
        var e = nueva(); var t = espera();
        mvc.perform(post("/encomiendas/" + e.getId() + "/asignar").param("turnoId", t.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/encomiendas/" + e.getId() + "/asignar").with(csrf()).with(user("cajero").roles("CAJA"))
                .param("turnoId", t.getId().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/encomiendas/" + e.getId() + "/asignar").with(csrf()).with(user("operador").roles("OPERATIVO"))
                .param("turnoId", t.getId().toString())).andExpect(status().is3xxRedirection());
    }

    int paralelos(Callable<?> primero, Callable<?> segundo) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var resultados = new ArrayList<Future<Boolean>>();
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
