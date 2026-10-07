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
    public Turno guardar(Turno turno) {

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