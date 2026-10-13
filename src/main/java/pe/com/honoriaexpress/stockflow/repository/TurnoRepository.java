package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.SentidoRuta;
import pe.com.honoriaexpress.stockflow.entity.Turno;

import java.time.LocalDate;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Turno t where t.id = :id")
    java.util.Optional<Turno> buscarParaActualizar(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select t from Turno t where t.estado in (pe.com.honoriaexpress.stockflow.entity.EstadoTurno.PROGRAMADO, pe.com.honoriaexpress.stockflow.entity.EstadoTurno.EN_ESPERA) and not exists (select s.id from Salida s where s.turno = t) order by t.fecha, t.sentidoRuta, t.numeroTurno")
    List<Turno> findColaPendiente();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Turno t where t.fecha = :fecha and t.sentidoRuta = :ruta order by t.numeroTurno")
    List<Turno> bloquearCola(@org.springframework.data.repository.query.Param("fecha") LocalDate fecha,
                            @org.springframework.data.repository.query.Param("ruta") SentidoRuta ruta);

    /*
     * Permite listar los turnos ordenados
     * primero por fecha y luego por número de turno.
     */
    List<Turno> findAllByOrderByFechaDescNumeroTurnoAsc();


    /*
     * Verifica si ya existe un turno con:
     *
     * - misma fecha
     * - misma ruta
     * - mismo número de turno
     *
     * Se utiliza durante el registro.
     */
    boolean existsByFechaAndSentidoRutaAndNumeroTurno(
            LocalDate fecha,
            SentidoRuta sentidoRuta,
            Integer numeroTurno
    );


    /*
     * Hace la misma validación anterior,
     * pero ignora el ID del turno que estamos editando.
     *
     * Esto evita falsos duplicados durante una actualización.
     */
    boolean existsByFechaAndSentidoRutaAndNumeroTurnoAndIdNot(
            LocalDate fecha,
            SentidoRuta sentidoRuta,
            Integer numeroTurno,
            Long id
    );


    /*
     * Comprueba si un conductor ya está asignado
     * a otro turno en la misma fecha y hora.
     */
    boolean existsByFechaAndHoraProgramadaAndConductorId(
            LocalDate fecha,
            java.time.LocalTime horaProgramada,
            Long conductorId
    );


    /*
     * La misma comprobación para edición,
     * ignorando el turno actual.
     */
    boolean existsByFechaAndHoraProgramadaAndConductorIdAndIdNot(
            LocalDate fecha,
            java.time.LocalTime horaProgramada,
            Long conductorId,
            Long id
    );


    /*
     * Comprueba si un vehículo ya está asignado
     * a otro turno en la misma fecha y hora.
     */
    boolean existsByFechaAndHoraProgramadaAndVehiculoId(
            LocalDate fecha,
            java.time.LocalTime horaProgramada,
            Long vehiculoId
    );


    /*
     * Comprobación equivalente durante la edición.
     */
    boolean existsByFechaAndHoraProgramadaAndVehiculoIdAndIdNot(
            LocalDate fecha,
            java.time.LocalTime horaProgramada,
            Long vehiculoId,
            Long id
    );
}
