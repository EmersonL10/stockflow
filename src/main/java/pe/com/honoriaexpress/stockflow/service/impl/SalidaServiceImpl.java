package pe.com.honoriaexpress.stockflow.service.impl;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Validator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.Salida;
import pe.com.honoriaexpress.stockflow.entity.Turno;
import pe.com.honoriaexpress.stockflow.entity.EstadoTurno;
import pe.com.honoriaexpress.stockflow.entity.EstadoSalida;
import pe.com.honoriaexpress.stockflow.exception.SalidaValidationException;
import pe.com.honoriaexpress.stockflow.repository.TurnoRepository;
import pe.com.honoriaexpress.stockflow.repository.SalidaRepository;
import pe.com.honoriaexpress.stockflow.service.SalidaService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalidaServiceImpl implements SalidaService {

    private final SalidaRepository salidaRepository;
    private final TurnoRepository turnoRepository;
    private final Validator validator;
    private final pe.com.honoriaexpress.stockflow.service.CajaService cajaService;

    @Override
    @Transactional(readOnly = true)
    public pe.com.honoriaexpress.stockflow.service.ResumenSalidas resumirDia(java.time.LocalDate fecha) {
        var registros = salidaRepository.consultarDia(fecha, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        long realizadas = 0, sinCuota = 0;
        var generadas = java.math.BigDecimal.ZERO.setScale(2);
        var pagadas = generadas;
        var pendientes = generadas;
        var cobrado = generadas;
        for (Salida s : registros) {
            boolean efectiva = s.getEstado() == EstadoSalida.EN_RUTA || s.getEstado() == EstadoSalida.FINALIZADA;
            if (efectiva && fecha.equals(s.getTurno().getFecha())) {
                realizadas++;
                if (s.getCuotaAgencia() == null) sinCuota++;
                else if (s.getEstadoCuota() != pe.com.honoriaexpress.stockflow.entity.EstadoCuota.ANULADA) {
                    generadas = generadas.add(s.getCuotaAgencia());
                    if (s.getEstadoCuota() == pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PAGADA) pagadas = pagadas.add(s.getCuotaAgencia());
                    if (s.getEstadoCuota() == pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PENDIENTE) pendientes = pendientes.add(s.getCuotaAgencia());
                }
            }
            if (s.getEstadoCuota() == pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PAGADA
                    && s.getCuotaAgencia() != null && s.getFechaPago() != null && fecha.equals(s.getFechaPago().toLocalDate())) {
                cobrado = cobrado.add(s.getCuotaAgencia());
            }
        }
        return new pe.com.honoriaexpress.stockflow.service.ResumenSalidas(realizadas, generadas, pagadas, pendientes, cobrado, sinCuota);
    }

    @org.springframework.beans.factory.annotation.Value("${app.agencia.cuota:10.00}")
    private java.math.BigDecimal tarifaAgencia = new java.math.BigDecimal("10.00");

    public java.math.BigDecimal getTarifaAgencia() {
        return tarifaAgencia;
    }

    private void sellarPago(Salida salida) {
        salida.setEstadoCuota(pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PAGADA);
        salida.setFechaPago(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        salida.setPagoRegistradoPor(auth == null ? "sistema" : auth.getName());
    }

    @Override
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR','CAJA')")
    public void registrarPago(Long id) {
        registrarPago(id, pe.com.honoriaexpress.stockflow.entity.MedioPago.EFECTIVO);
    }

    @Override
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR','CAJA')")
    public void registrarPago(Long id, pe.com.honoriaexpress.stockflow.entity.MedioPago medio) {
        Salida salida = salidaRepository.buscarParaCobro(id).orElseThrow(
                () -> new SalidaValidationException("id", "La salida no existe."));
        if (salida.getEstadoCuota() != pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PENDIENTE
                || salida.getCuotaAgencia() == null || salida.getEstado() == EstadoSalida.CANCELADA) {
            throw new SalidaValidationException("estado", "Esta salida no tiene una cuota pendiente de pago.");
        }
        sellarPago(salida);
        salidaRepository.saveAndFlush(salida);
        cajaService.registrarCobroSalida(salida, medio);
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    public List<Salida> listarTodos() {
        return salidaRepository.findAllByOrderByIdDesc();
    }

    @Override
    public Optional<Salida> buscarPorId(Long id) {
        return salidaRepository.findById(id);
    }

    @Override
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO') and (!#salida.pagoAlSalir or hasAnyRole('ADMINISTRADOR','CAJA'))")
    public Salida guardar(Salida salida) {
        boolean nuevoCobro = false;
        Salida original = salida.getId() == null ? null : salidaRepository.buscarParaCobro(salida.getId())
                .orElseThrow(() -> new SalidaValidationException("id", "La salida solicitada no existe."));
        if (original == null) {
            salida.setEstado(EstadoSalida.EN_RUTA);
            salida.setMontoRecaudado(null);
        } else {
            salida.setMontoRecaudado(original.getMontoRecaudado());
        }
        var errores = validator.validate(salida);
        if (!errores.isEmpty()) {
            var error = errores.iterator().next();
            throw new SalidaValidationException(error.getPropertyPath().toString(), error.getMessage());
        }
        if (salida.getTurno().getId() == null) {
            throw new SalidaValidationException("turno", "Seleccione un turno existente.");
        }
        if (salida.getId() != null && !salidaRepository.existsById(salida.getId())) {
            throw new SalidaValidationException("id", "La salida solicitada no existe.");
        }
        Turno turno = turnoRepository.findById(salida.getTurno().getId())
                .orElseThrow(() -> new SalidaValidationException("turno", "El turno seleccionado no existe."));
        var cola = turnoRepository.bloquearCola(turno.getFecha(), turno.getSentidoRuta());
        // Una espera por el bloqueo puede dejar entidades ya cargadas desactualizadas.
        if (entityManager != null) {
            cola.forEach(entityManager::refresh);
        }
        if (original != null) {
            Salida anterior = original;
            if (!anterior.getTurno().getId().equals(turno.getId())) {
                throw new SalidaValidationException("turno", "No se puede cambiar el turno de una salida registrada.");
            }
            if ((anterior.getEstado() == EstadoSalida.FINALIZADA || anterior.getEstado() == EstadoSalida.CANCELADA)
                    && anterior.getEstado() != salida.getEstado()) {
                throw new SalidaValidationException("estado", "No se puede reabrir una salida finalizada o cancelada.");
            }
            if (anterior.getEstado() != EstadoSalida.PROGRAMADA && salida.getEstado() == EstadoSalida.PROGRAMADA) {
                throw new SalidaValidationException("estado", "Una salida ejecutada no puede regresar a PROGRAMADA.");
            }
        } else if (turno.getEstado() == EstadoTurno.CANCELADO || turno.getEstado() == EstadoTurno.COMPLETADO) {
            throw new SalidaValidationException("turno", "El turno ya no está en espera.");
        }
        boolean duplicado = salida.getId() == null
                ? salidaRepository.existsByTurnoId(turno.getId())
                : salidaRepository.existsByTurnoIdAndIdNot(turno.getId(), salida.getId());
        if (duplicado) {
            throw new SalidaValidationException("turno", "El turno seleccionado ya tiene una salida registrada.");
        }
        if (salida.getCantidadPasajeros() > turno.getVehiculo().getCapacidadPasajeros()) {
            throw new SalidaValidationException("cantidadPasajeros", "La cantidad de pasajeros supera la capacidad del vehículo.");
        }
        salida.setTurno(turno);
        if (original == null || (original.getEstado() == EstadoSalida.PROGRAMADA
                && (salida.getEstado() == EstadoSalida.EN_RUTA || salida.getEstado() == EstadoSalida.FINALIZADA))) {
            if (tarifaAgencia == null || tarifaAgencia.signum() < 0 || tarifaAgencia.scale() > 2
                    || tarifaAgencia.compareTo(new java.math.BigDecimal("99999999.99")) > 0) {
                throw new IllegalStateException("La tarifa de agencia debe ser un importe válido con hasta dos decimales.");
            }
            salida.setCuotaAgencia(tarifaAgencia);
            salida.setEstadoCuota(pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PENDIENTE);
            salida.setFechaPago(null);
            salida.setPagoRegistradoPor(null);
            if (salida.isPagoAlSalir()) { sellarPago(salida); nuevoCobro = true; }
        } else {
            salida.setCuotaAgencia(original.getCuotaAgencia());
            salida.setEstadoCuota(original.getEstadoCuota());
            salida.setFechaPago(original.getFechaPago());
            salida.setPagoRegistradoPor(original.getPagoRegistradoPor());
        }
        if (salida.getEstado() == EstadoSalida.CANCELADA && salida.getCuotaAgencia() != null) {
            if (salida.getEstadoCuota() == pe.com.honoriaexpress.stockflow.entity.EstadoCuota.PAGADA) {
                throw new SalidaValidationException("estado", "Una cuota pagada requiere regularización en Caja antes de cancelar.");
            }
            salida.setEstadoCuota(pe.com.honoriaexpress.stockflow.entity.EstadoCuota.ANULADA);
        }
        if (salida.getRutaReal() == null || salida.getRutaReal().isBlank()) {
            salida.setRutaReal(turno.getSentidoRuta() == pe.com.honoriaexpress.stockflow.entity.SentidoRuta.PUCALLPA_HONORIA
                    ? "Pucallpa - Honoria" : "Honoria - Pucallpa");
        } else {
            salida.setRutaReal(salida.getRutaReal().strip());
        }
        if (salida.getEstado() == EstadoSalida.EN_RUTA || salida.getEstado() == EstadoSalida.FINALIZADA) {
            turno.setEstado(EstadoTurno.COMPLETADO);
        } else if (salida.getEstado() == EstadoSalida.CANCELADA) {
            turno.setEstado(EstadoTurno.CANCELADO);
        }
        Salida guardada = salidaRepository.saveAndFlush(salida);
        if (nuevoCobro) cajaService.registrarCobroSalida(guardada, salida.getMedioPago());
        return guardada;
    }

    @Override
    public boolean existePorTurno(Long turnoId) {
        return salidaRepository.existsByTurnoId(turnoId);
    }

    @Override
    public boolean existePorTurnoYIdDiferente(Long turnoId, Long id) {
        return salidaRepository.existsByTurnoIdAndIdNot(turnoId, id);
    }
}
