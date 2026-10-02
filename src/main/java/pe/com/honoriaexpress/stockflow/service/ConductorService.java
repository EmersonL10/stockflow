package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.Conductor;

import java.util.List;
import java.util.Optional;

public interface ConductorService {

    List<Conductor> listarTodos();

    Optional<Conductor> buscarPorId(Long id);

    Conductor guardar(Conductor conductor);

    boolean existePorDni(String dni);

    boolean existePorLicencia(String licencia);

    boolean existeDniEnOtroConductor(String dni, Long id);

    boolean existeLicenciaEnOtroConductor(String licencia, Long id);

    void cambiarEstado(Long id);
}