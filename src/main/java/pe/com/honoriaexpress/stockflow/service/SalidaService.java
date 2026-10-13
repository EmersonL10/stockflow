package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.Salida;

import java.util.List;
import java.util.Optional;

public interface SalidaService {
    ResumenSalidas resumirDia(java.time.LocalDate fecha);
    java.math.BigDecimal getTarifaAgencia();
    void registrarPago(Long id);

    List<Salida> listarTodos();

    Optional<Salida> buscarPorId(Long id);

    Salida guardar(Salida salida);

    boolean existePorTurno(Long turnoId);

    boolean existePorTurnoYIdDiferente(Long turnoId, Long id);
}
