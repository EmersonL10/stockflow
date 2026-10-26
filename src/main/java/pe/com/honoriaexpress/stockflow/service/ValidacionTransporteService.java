package pe.com.honoriaexpress.stockflow.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.com.honoriaexpress.stockflow.entity.Turno;
import pe.com.honoriaexpress.stockflow.exception.SalidaValidationException;
import pe.com.honoriaexpress.stockflow.repository.ConductorRepository;
import pe.com.honoriaexpress.stockflow.repository.VehiculoRepository;

@Service
@RequiredArgsConstructor
public class ValidacionTransporteService {
    private final ConductorRepository conductores;
    private final VehiculoRepository vehiculos;

    @PersistenceContext
    private EntityManager entityManager;

    /** Mantiene los registros habilitados hasta que termine la operación que los utiliza. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void validar(Turno turno) {
        if (turno.getConductor() == null || turno.getConductor().getId() == null) {
            throw new SalidaValidationException("turno", "Seleccione un conductor existente y activo.");
        }
        if (turno.getVehiculo() == null || turno.getVehiculo().getId() == null) {
            throw new SalidaValidationException("turno", "Seleccione un vehículo existente y activo.");
        }
        var conductor = conductores.buscarParaOperacion(turno.getConductor().getId()).orElseThrow(
                () -> new SalidaValidationException("turno", "El conductor seleccionado no existe."));
        // El formulario o el turno pueden haber cargado el estado antes de esperar un bloqueo.
        entityManager.refresh(conductor, LockModeType.PESSIMISTIC_READ);
        if (!Boolean.TRUE.equals(conductor.getActivo())) {
            throw new SalidaValidationException("turno", "El conductor está inactivo. Seleccione un conductor activo para continuar.");
        }
        var vehiculo = vehiculos.buscarParaOperacion(turno.getVehiculo().getId()).orElseThrow(
                () -> new SalidaValidationException("turno", "El vehículo seleccionado no existe."));
        entityManager.refresh(vehiculo, LockModeType.PESSIMISTIC_READ);
        if (!Boolean.TRUE.equals(vehiculo.getActivo())) {
            throw new SalidaValidationException("turno", "El vehículo está inactivo. Seleccione un vehículo activo para continuar.");
        }
        turno.setConductor(conductor);
        turno.setVehiculo(vehiculo);
    }

    public boolean estaActivo(Turno turno) {
        return turno.getConductor() != null && Boolean.TRUE.equals(turno.getConductor().getActivo())
                && turno.getVehiculo() != null && Boolean.TRUE.equals(turno.getVehiculo().getActivo());
    }
}
