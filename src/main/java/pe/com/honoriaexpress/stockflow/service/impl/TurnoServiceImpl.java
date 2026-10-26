package pe.com.honoriaexpress.stockflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.SentidoRuta;
import pe.com.honoriaexpress.stockflow.entity.Turno;
import pe.com.honoriaexpress.stockflow.repository.TurnoRepository;
import pe.com.honoriaexpress.stockflow.service.TurnoService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TurnoServiceImpl implements TurnoService {

    private final TurnoRepository turnoRepository;
    private final pe.com.honoriaexpress.stockflow.repository.SalidaRepository salidaRepository;
    private final pe.com.honoriaexpress.stockflow.repository.EncomiendaRepository encomiendas;
    private final pe.com.honoriaexpress.stockflow.service.ValidacionTransporteService validacionTransporte;

    @Override
    public List<Turno> listarColaPendiente() {
        return turnoRepository.findColaPendienteDelDia(pe.com.honoriaexpress.stockflow.service.FechaOperativa.hoy())
                .stream().filter(validacionTransporte::estaActivo).toList();
    }


    @Override
    public List<Turno> listarTodos() {

        return turnoRepository
                .findAllByOrderByFechaDescNumeroTurnoAsc();
    }


    @Override
    public Optional<Turno> buscarPorId(Long id) {

        return turnoRepository.findById(id);
    }


    @Override
    @org.springframework.transaction.annotation.Transactional
    public Turno guardar(Turno turno) {
        if (!pe.com.honoriaexpress.stockflow.service.FechaOperativa.hoy().equals(turno.getFecha())) {
            throw new pe.com.honoriaexpress.stockflow.exception.SalidaValidationException("fecha", "Las llegadas se registran con la fecha de hoy. Los turnos antiguos se conservan en el historial.");
        }
        if (turno.getId() != null) {
            var original = turnoRepository.buscarParaActualizar(turno.getId()).orElseThrow(
                    () -> new pe.com.honoriaexpress.stockflow.exception.SalidaValidationException("id", "El turno no existe."));
            if (!original.getFecha().equals(turno.getFecha())) {
                throw new pe.com.honoriaexpress.stockflow.exception.SalidaValidationException("fecha", "La fecha original de una llegada no se modifica. Registre una nueva llegada para hoy.");
            }
            if (encomiendas.existsByTurnoAsignadoIdAndEstado(turno.getId(), pe.com.honoriaexpress.stockflow.entity.EstadoEncomienda.ASIGNADA)) {
                throw new pe.com.honoriaexpress.stockflow.exception.SalidaValidationException("id", "Retire las encomiendas asignadas antes de modificar el turno.");
            }
            if (salidaRepository.existsByTurnoId(turno.getId())) {
                throw new pe.com.honoriaexpress.stockflow.exception.SalidaValidationException("id", "El turno tiene una salida registrada y no se puede modificar. Consulte su historial en Salidas.");
            }
        }
        validacionTransporte.validar(turno);
        if (turno.getRutaOperativa() != null && !turno.getRutaOperativa().isBlank()) {
            turno.setRutaOperativa(turno.getRutaOperativa().strip());
            turno.setSentidoRuta(pe.com.honoriaexpress.stockflow.service.Rutas.coinciden(turno.getRutaOperativa(), "Honoria - Pucallpa") ? SentidoRuta.HONORIA_PUCALLPA : SentidoRuta.PUCALLPA_HONORIA);
        }
        return turnoRepository.save(turno);
    }


    @Override
    public boolean existeTurno(
            LocalDate fecha,
            SentidoRuta sentidoRuta,
            Integer numeroTurno) {

        return turnoRepository
                .existsByFechaAndSentidoRutaAndNumeroTurno(
                        fecha,
                        sentidoRuta,
                        numeroTurno
                );
    }


    @Override
    public boolean existeTurnoEnOtroRegistro(
            LocalDate fecha,
            SentidoRuta sentidoRuta,
            Integer numeroTurno,
            Long id) {

        return turnoRepository
                .existsByFechaAndSentidoRutaAndNumeroTurnoAndIdNot(
                        fecha,
                        sentidoRuta,
                        numeroTurno,
                        id
                );
    }


    @Override
    public boolean conductorOcupado(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long conductorId) {

        return turnoRepository
                .existsByFechaAndHoraProgramadaAndConductorId(
                        fecha,
                        horaProgramada,
                        conductorId
                );
    }


    @Override
    public boolean conductorOcupadoEnOtroTurno(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long conductorId,
            Long id) {

        return turnoRepository
                .existsByFechaAndHoraProgramadaAndConductorIdAndIdNot(
                        fecha,
                        horaProgramada,
                        conductorId,
                        id
                );
    }


    @Override
    public boolean vehiculoOcupado(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long vehiculoId) {

        return turnoRepository
                .existsByFechaAndHoraProgramadaAndVehiculoId(
                        fecha,
                        horaProgramada,
                        vehiculoId
                );
    }


    @Override
    public boolean vehiculoOcupadoEnOtroTurno(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long vehiculoId,
            Long id) {

        return turnoRepository
                .existsByFechaAndHoraProgramadaAndVehiculoIdAndIdNot(
                        fecha,
                        horaProgramada,
                        vehiculoId,
                        id
                );
    }
}
