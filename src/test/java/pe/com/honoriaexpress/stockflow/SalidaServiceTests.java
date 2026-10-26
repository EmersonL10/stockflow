package pe.com.honoriaexpress.stockflow;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.exception.SalidaValidationException;
import pe.com.honoriaexpress.stockflow.repository.SalidaRepository;
import pe.com.honoriaexpress.stockflow.repository.TurnoRepository;
import pe.com.honoriaexpress.stockflow.service.impl.SalidaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalidaServiceTests {
    static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    SalidaRepository salidas;
    TurnoRepository turnos;
    SalidaServiceImpl service;
    Salida salida;
    Turno turno;

    @BeforeEach
    void preparar() {
        salidas = mock(SalidaRepository.class);
        turnos = mock(TurnoRepository.class);
        service = new SalidaServiceImpl(salidas, turnos, factory.getValidator(),
                mock(pe.com.honoriaexpress.stockflow.service.CajaService.class),
                mock(pe.com.honoriaexpress.stockflow.repository.EncomiendaRepository.class),
                mock(pe.com.honoriaexpress.stockflow.repository.EncomiendaEventoRepository.class),
                mock(pe.com.honoriaexpress.stockflow.service.ValidacionTransporteService.class));
        turno = Turno.builder().id(1L).fecha(pe.com.honoriaexpress.stockflow.service.FechaOperativa.hoy())
                .sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).numeroTurno(1)
                .vehiculo(Vehiculo.builder().capacidadPasajeros(10).build()).build();
        when(turnos.bloquearCola(turno.getFecha(), turno.getSentidoRuta())).thenReturn(java.util.List.of(turno));
        salida = Salida.builder().turno(Turno.builder().id(1L).build())
                .horaSalidaReal(LocalTime.of(8, 0)).cantidadPasajeros(10)
                .montoRecaudado(new BigDecimal("100.00")).build();
    }

    @AfterAll
    static void cerrar() { factory.close(); }

    void rechazar() {
        assertThrows(SalidaValidationException.class, () -> service.guardar(salida));
        verify(salidas, never()).saveAndFlush(any());
    }

    @Test void rechazaMontoNegativo() { salida.setMontoRecaudado(new BigDecimal("-1")); assertFalse(factory.getValidator().validate(salida).isEmpty()); }
    @Test void rechazaDecimalesExcesivos() { salida.setMontoRecaudado(new BigDecimal("1.001")); assertFalse(factory.getValidator().validate(salida).isEmpty()); }
    @Test void rechazaMontoFueraDeRango() { salida.setMontoRecaudado(new BigDecimal("100000000")); assertFalse(factory.getValidator().validate(salida).isEmpty()); }
    @Test void rechazaPasajerosNegativos() { salida.setCantidadPasajeros(-1); rechazar(); }
    @Test void rechazaEstadoNulo() { salida.setEstado(null); assertFalse(factory.getValidator().validate(salida).isEmpty()); }
    @Test void rechazaTurnoSinId() { salida.getTurno().setId(null); rechazar(); }
    @Test void rechazaTurnoInexistente() { rechazar(); }
    @Test void rechazaEdicionInexistente() { salida.setId(20L); rechazar(); }

    @Test void rechazaExcesoDeCapacidad() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        salida.setCantidadPasajeros(11);
        rechazar();
    }

    @Test void rechazaTurnoDuplicado() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        when(salidas.existsByTurnoId(1L)).thenReturn(true);
        rechazar();
    }

    @Test void guardaUsandoTurnoPersistidoYCapacidadExacta() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        when(salidas.saveAndFlush(salida)).thenReturn(salida);
        assertSame(salida, service.guardar(salida));
        assertSame(turno, salida.getTurno());
    }

    @Test void editarExcluyeSuPropioId() {
        salida.setId(20L);
        when(salidas.existsById(20L)).thenReturn(true);
        when(salidas.buscarParaCobro(20L)).thenReturn(Optional.of(salida));
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        service.guardar(salida);
        verify(salidas).existsByTurnoIdAndIdNot(1L, 20L);
        verify(salidas).saveAndFlush(salida);
    }

    @Test void generaCuotaAutomaticaSinAceptarImporteDelCliente() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        salida.setCuotaAgencia(new BigDecimal("1.00"));
        salida.setEstadoCuota(EstadoCuota.PAGADA);
        service.guardar(salida);
        assertEquals(new BigDecimal("10.00"), salida.getCuotaAgencia());
        assertEquals(EstadoCuota.PENDIENTE, salida.getEstadoCuota());
        assertNull(salida.getMontoRecaudado());
        assertEquals(EstadoSalida.EN_RUTA, salida.getEstado());
    }

    @Test void editarConservaCuotaYRecaudacionHistorica() {
        var anterior = Salida.builder().id(20L).turno(turno).estado(EstadoSalida.EN_RUTA)
                .cuotaAgencia(new BigDecimal("10.00")).estadoCuota(EstadoCuota.PENDIENTE)
                .montoRecaudado(new BigDecimal("80.00")).build();
        salida.setId(20L);
        salida.setEstado(EstadoSalida.FINALIZADA);
        turno.setEstado(EstadoTurno.COMPLETADO);
        when(salidas.buscarParaCobro(20L)).thenReturn(Optional.of(anterior));
        when(salidas.existsById(20L)).thenReturn(true);
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        service.guardar(salida);
        assertEquals(anterior.getCuotaAgencia(), salida.getCuotaAgencia());
        assertEquals(anterior.getMontoRecaudado(), salida.getMontoRecaudado());
    }

    @Test void cancelarAnulaCuotaPendiente() {
        var anterior = Salida.builder().id(20L).turno(turno).estado(EstadoSalida.EN_RUTA)
                .cuotaAgencia(new BigDecimal("10.00")).estadoCuota(EstadoCuota.PENDIENTE).build();
        salida.setId(20L);
        salida.setEstado(EstadoSalida.CANCELADA);
        when(salidas.buscarParaCobro(20L)).thenReturn(Optional.of(anterior));
        when(salidas.existsById(20L)).thenReturn(true);
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        service.guardar(salida);
        assertEquals(EstadoCuota.ANULADA, salida.getEstadoCuota());
    }

    @Test void permitePagarAlSalir() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        salida.setPagoAlSalir(true);
        service.guardar(salida);
        assertEquals(EstadoCuota.PAGADA, salida.getEstadoCuota());
        assertNotNull(salida.getFechaPago());
        assertNotNull(salida.getPagoRegistradoPor());
    }

    @Test void resumenDistingueFechaDeSalidaYFechaDeCobro() {
        var fecha = turno.getFecha();
        var ayer = Turno.builder().fecha(fecha.minusDays(1)).build();
        var pendiente = Salida.builder().turno(turno).estado(EstadoSalida.EN_RUTA)
                .cuotaAgencia(new BigDecimal("10.00")).estadoCuota(EstadoCuota.PENDIENTE).build();
        var pagada = Salida.builder().turno(turno).estado(EstadoSalida.FINALIZADA)
                .cuotaAgencia(new BigDecimal("10.00")).estadoCuota(EstadoCuota.PAGADA).fechaPago(fecha.atTime(9,0)).build();
        var anterior = Salida.builder().turno(ayer).estado(EstadoSalida.FINALIZADA)
                .cuotaAgencia(new BigDecimal("10.00")).estadoCuota(EstadoCuota.PAGADA).fechaPago(fecha.atTime(10,0)).build();
        var historica = Salida.builder().turno(turno).estado(EstadoSalida.EN_RUTA).build();
        when(salidas.consultarDia(fecha, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay()))
                .thenReturn(java.util.List.of(pendiente,pagada,anterior,historica));
        var resumen = service.resumirDia(fecha);
        assertEquals(3, resumen.realizadas());
        assertEquals(new BigDecimal("20.00"), resumen.generadas());
        assertEquals(new BigDecimal("10.00"), resumen.pagadas());
        assertEquals(new BigDecimal("10.00"), resumen.pendientes());
        assertEquals(new BigDecimal("20.00"), resumen.cobradoHoy());
        assertEquals(1, resumen.sinCuota());
    }

    @Test void permitePagarAlRegreso() {
        salida.setCuotaAgencia(new BigDecimal("10.00"));
        salida.setEstadoCuota(EstadoCuota.PENDIENTE);
        when(salidas.buscarParaCobro(1L)).thenReturn(Optional.of(salida));
        service.registrarPago(1L);
        assertEquals(EstadoCuota.PAGADA, salida.getEstadoCuota());
        assertNotNull(salida.getFechaPago());
        verify(salidas).saveAndFlush(salida);
    }

    @Test void impideRegistrarDosVecesElPago() {
        salida.setCuotaAgencia(new BigDecimal("10.00"));
        salida.setEstadoCuota(EstadoCuota.PAGADA);
        when(salidas.buscarParaCobro(1L)).thenReturn(Optional.of(salida));
        assertThrows(SalidaValidationException.class, () -> service.registrarPago(1L));
        verify(salidas, never()).saveAndFlush(any());
    }

    @Test void cancelarNoBorraCuotaPagada() {
        var anterior = Salida.builder().id(20L).turno(turno).estado(EstadoSalida.EN_RUTA)
                .cuotaAgencia(new BigDecimal("10.00")).estadoCuota(EstadoCuota.PAGADA).build();
        salida.setId(20L);
        salida.setEstado(EstadoSalida.CANCELADA);
        when(salidas.buscarParaCobro(20L)).thenReturn(Optional.of(anterior));
        when(salidas.existsById(20L)).thenReturn(true);
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        rechazar();
    }

    @Test void salirCompletaTurnoYLiberaSiguiente() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        salida.setEstado(EstadoSalida.EN_RUTA);
        service.guardar(salida);
        assertEquals(EstadoTurno.COMPLETADO, turno.getEstado());
    }

    @Test void conservaRutaEscritaPorOperador() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        salida.setRutaReal("  Pucallpa - Campo Verde  ");
        service.guardar(salida);
        assertEquals("Pucallpa - Campo Verde", salida.getRutaReal());
    }

    @Test void usaRutaPlanificadaCuandoNoSeEscribeOtra() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        service.guardar(salida);
        assertEquals("Pucallpa - Honoria", salida.getRutaReal());
    }

    @Test void permiteElegirVehiculoAunqueNoSeaElPrimero() {
        when(turnos.findById(1L)).thenReturn(Optional.of(turno));
        var primero = Turno.builder().id(2L).numeroTurno(1).build();
        when(turnos.bloquearCola(turno.getFecha(), turno.getSentidoRuta())).thenReturn(java.util.List.of(primero, turno));
        salida.setEstado(EstadoSalida.EN_RUTA);
        service.guardar(salida);
        verify(salidas).saveAndFlush(salida);
        assertEquals(EstadoTurno.COMPLETADO, turno.getEstado());
        assertEquals(EstadoTurno.PROGRAMADO, primero.getEstado());
        assertEquals(new BigDecimal("10.00"), salida.getCuotaAgencia());
    }
}
