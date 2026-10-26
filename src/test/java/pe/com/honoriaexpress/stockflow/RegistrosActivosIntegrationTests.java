package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.honoriaexpress.stockflow.dto.EncomiendaFormulario;
import pe.com.honoriaexpress.stockflow.dto.EntregaEncomiendaFormulario;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.exception.EncomiendaOperacionException;
import pe.com.honoriaexpress.stockflow.exception.SalidaValidationException;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMINISTRADOR")
class RegistrosActivosIntegrationTests {
    @Autowired TurnoService turnoService;
    @Autowired SalidaService salidaService;
    @Autowired EncomiendaService encomiendaService;
    @Autowired CajaService cajaService;
    @Autowired InicioService inicioService;
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

    @BeforeEach void preparar() {
        eventos.deleteAll(); movimientos.deleteAll(); encomiendas.deleteAll(); cajas.deleteAll();
        salidas.deleteAll(); turnos.deleteAll(); vehiculos.deleteAll(); conductores.deleteAll();
        conductor = conductores.saveAndFlush(Conductor.builder().dni("00000008").nombres("Prueba")
                .apellidos("Registros activos").licencia("TEST-ACTIVOS").build());
        vehiculo = vehiculos.saveAndFlush(Vehiculo.builder().placa("QA-A01").marca("PRUEBA")
                .modelo("ACTIVOS").anio(2020).color("BLANCO").capacidadPasajeros(5).numeroTuc("QA-ACTIVOS").build());
        cajaService.abrir(BigDecimal.ZERO);
    }

    private Turno llegada() {
        return Turno.builder().fecha(FechaOperativa.hoy()).numeroTurno(1)
                .sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).horaProgramada(LocalTime.NOON)
                .conductor(conductor).vehiculo(vehiculo).build();
    }

    private Salida salida(Turno turno) {
        return Salida.builder().turno(Turno.builder().id(turno.getId()).build())
                .horaSalidaReal(LocalTime.of(12, 30)).cantidadPasajeros(4).build();
    }

    private void inactivar(boolean esConductor) {
        if (esConductor) {
            var actual = conductores.findById(conductor.getId()).orElseThrow();
            actual.setActivo(false); conductores.saveAndFlush(actual);
        } else {
            var actual = vehiculos.findById(vehiculo.getId()).orElseThrow();
            actual.setActivo(false); vehiculos.saveAndFlush(actual);
        }
    }

    private Encomienda registrarPaquete() {
        var f = new EncomiendaFormulario();
        f.setRemitente("Remitente de prueba"); f.setDocumentoRemitente("00000001");
        f.setTelefonoRemitente("900000001"); f.setDestinatario("Destinatario de prueba");
        f.setDocumentoDestinatario("00000002"); f.setTelefonoDestinatario("900000002");
        f.setRuta("Pucallpa - Honoria"); f.setDescripcion("Contenido de prueba");
        f.setImporte(new BigDecimal("25.00")); f.setSolicitud(UUID.randomUUID().toString());
        return encomiendaService.registrar(f);
    }

    @Test void permiteLlegadaYSalidaConRegistrosActivos() {
        var turno = turnoService.guardar(llegada());
        var viaje = salidaService.guardar(salida(turno));
        assertEquals(EstadoSalida.EN_RUTA, viaje.getEstado());
        assertEquals(EstadoCuota.PENDIENTE, viaje.getEstadoCuota());
        assertEquals(new BigDecimal("10.00"), viaje.getCuotaAgencia());
    }

    @Test void rechazaReferenciaInexistenteAntesDeGuardarLaLlegada() {
        var turno = llegada();
        turno.setConductor(Conductor.builder().id(Long.MAX_VALUE).build());
        assertThrows(SalidaValidationException.class, () -> turnoService.guardar(turno));
        assertEquals(0, turnos.count());
    }

    @Test void formularioIndicaInactivosYRechazaUnaSalidaEnviadaPorHttp() throws Exception {
        var turno = turnoService.guardar(llegada());
        inactivar(true);
        mvc.perform(get("/turnos/nuevo"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("(Inactivo)")));
        mvc.perform(post("/salidas/guardar").with(csrf())
                        .param("turno.id", turno.getId().toString())
                        .param("horaSalidaReal", "12:30").param("cantidadPasajeros", "4")
                        .param("estado", "EN_RUTA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El conductor está inactivo")));
        assertEquals(0, salidas.count());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void rechazaLlegadaAunqueElFormularioDeclareActivoUnRegistroInactivo(boolean esConductor) {
        inactivar(esConductor);
        // Las referencias originales conservan activo=true: el servicio debe consultar MySQL.
        assertTrue(conductor.getActivo()); assertTrue(vehiculo.getActivo());
        var error = assertThrows(SalidaValidationException.class, () -> turnoService.guardar(llegada()));
        assertTrue(error.getMessage().contains(esConductor ? "conductor" : "vehículo"));
        assertEquals(0, turnos.count());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void rechazaSalidaSinGenerarCuotaCobroNiCompletarTurno(boolean esConductor) {
        var turno = turnoService.guardar(llegada());
        inactivar(esConductor);
        var solicitud = salida(turno); solicitud.setPagoAlSalir(true);
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(solicitud));
        assertEquals(0, salidas.count()); assertEquals(0, movimientos.count());
        assertEquals(EstadoTurno.PROGRAMADO, turnos.findById(turno.getId()).orElseThrow().getEstado());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void excluyeInactivosDeSeleccionYContadorPeroConservaHistorial(boolean esConductor) {
        turnoService.guardar(llegada());
        inactivar(esConductor);
        assertTrue(turnoService.listarColaPendiente().isEmpty());
        assertTrue(encomiendaService.turnosDisponibles("Pucallpa - Honoria").isEmpty());
        assertEquals(0, inicioService.consultar().enEspera());
        assertEquals(1, turnoService.listarTodos().size());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void rechazaAsignacionSinModificarPaqueteNiRegistrarEvento(boolean esConductor) {
        var turno = turnoService.guardar(llegada());
        var paquete = registrarPaquete();
        long eventosAntes = eventos.count();
        inactivar(esConductor);
        assertThrows(EncomiendaOperacionException.class, () -> encomiendaService.asignar(paquete.getId(), turno.getId()));
        var actual = encomiendaService.buscar(paquete.getId());
        assertEquals(EstadoEncomienda.RECIBIDA, actual.getEstado());
        assertNull(actual.getTurnoAsignado()); assertEquals(eventosAntes, eventos.count());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void revalidaTransporteAlSalirAunqueLaCargaYaEstuvieraAsignada(boolean esConductor) {
        var turno = turnoService.guardar(llegada());
        var paquete = registrarPaquete();
        encomiendaService.asignar(paquete.getId(), turno.getId());
        inactivar(esConductor);
        var solicitud = salida(turno);
        solicitud.setCargaConfirmada(true); solicitud.setCargaIds(paquete.getId().toString());
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(solicitud));
        assertEquals(EstadoEncomienda.ASIGNADA, encomiendaService.buscar(paquete.getId()).getEstado());
        assertEquals(0, salidas.count());
        encomiendaService.retirarAsignacion(paquete.getId());
        assertEquals(EstadoEncomienda.RECIBIDA, encomiendaService.buscar(paquete.getId()).getEstado());
    }

    @Test void noPermiteEjecutarUnaSalidaProgramadaConTransporteInactivo() {
        var turno = turnoService.guardar(llegada());
        var programada = salida(turno); programada.setTurno(turno);
        var guardada = salidas.saveAndFlush(programada);
        inactivar(true);
        var ejecutar = salida(turno); ejecutar.setId(guardada.getId()); ejecutar.setEstado(EstadoSalida.EN_RUTA);
        assertThrows(SalidaValidationException.class, () -> salidaService.guardar(ejecutar));
        assertEquals(EstadoSalida.PROGRAMADA, salidas.findById(guardada.getId()).orElseThrow().getEstado());
        assertEquals(0, movimientos.count());
    }

    @Test void permiteCobrarYFinalizarUnViajeYaEjecutadoAunqueSeInactiveElTransporte() {
        var turno = turnoService.guardar(llegada());
        var viaje = salidaService.guardar(salida(turno));
        inactivar(true); inactivar(false);
        salidaService.registrarPago(viaje.getId());
        var editar = salida(turno); editar.setId(viaje.getId()); editar.setEstado(EstadoSalida.FINALIZADA);
        salidaService.guardar(editar);
        var actual = salidas.findById(viaje.getId()).orElseThrow();
        assertEquals(EstadoSalida.FINALIZADA, actual.getEstado());
        assertEquals(EstadoCuota.PAGADA, actual.getEstadoCuota());
        assertEquals(conductor.getId(), actual.getTurno().getConductor().getId());
        assertEquals(vehiculo.getId(), actual.getTurno().getVehiculo().getId());
        assertEquals(1, movimientos.count());
    }

    @Test void permiteCompletarEntregaDeUnPaqueteQueYaEstabaEnTransito() {
        var turno = turnoService.guardar(llegada());
        var paquete = registrarPaquete();
        encomiendaService.asignar(paquete.getId(), turno.getId());
        var solicitud = salida(turno);
        solicitud.setCargaConfirmada(true); solicitud.setCargaIds(paquete.getId().toString());
        salidaService.guardar(solicitud);
        inactivar(true); inactivar(false);
        encomiendaService.recibirDestino(paquete.getId());
        encomiendaService.cobrar(paquete.getId(), MedioPago.EFECTIVO);
        encomiendaService.entregar(paquete.getId(), new EntregaEncomiendaFormulario("Destinatario de prueba", "00000002"));
        assertEquals(EstadoEncomienda.ENTREGADA, encomiendaService.buscar(paquete.getId()).getEstado());
    }
}
