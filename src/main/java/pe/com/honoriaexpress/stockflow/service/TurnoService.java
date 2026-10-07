package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.SentidoRuta;
import pe.com.honoriaexpress.stockflow.entity.Turno;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TurnoService {

    List<Turno> listarTodos();

    Optional<Turno> buscarPorId(Long id);

    Turno guardar(Turno turno);


    boolean existeTurno(
            LocalDate fecha,
            SentidoRuta sentidoRuta,
            Integer numeroTurno
    );


    boolean existeTurnoEnOtroRegistro(
            LocalDate fecha,
            SentidoRuta sentidoRuta,
            Integer numeroTurno,
            Long id
    );


    boolean conductorOcupado(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long conductorId
    );


    boolean conductorOcupadoEnOtroTurno(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long conductorId,
            Long id
    );


    boolean vehiculoOcupado(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long vehiculoId
    );


    boolean vehiculoOcupadoEnOtroTurno(
            LocalDate fecha,
            LocalTime horaProgramada,
            Long vehiculoId,
            Long id
    );
}