package pe.com.honoriaexpress.stockflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service @RequiredArgsConstructor
public class InicioService {
    private final TurnoRepository turnos;
    private final SalidaRepository salidas;
    private final EncomiendaRepository encomiendas;
    private final VehiculoRepository vehiculos;
    private final MovimientoCajaRepository movimientos;

    public record Resumen(LocalDate fecha, long enEspera, long salidasHoy, long enviosActivos, BigDecimal cuotasPendientes, long vehiculosActivos) { }

    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR','CAJA')")
    public BigDecimal ingresosHoy() {
        LocalDate hoy = FechaOperativa.hoy();
        BigDecimal total = movimientos.sumarIngresosDelPeriodo(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        return total == null ? new BigDecimal("0.00") : total.setScale(2);
    }

    @Transactional(readOnly = true)
    public Resumen consultar() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        BigDecimal pendientes = salidas.sumarCuotasPendientes();
        return new Resumen(hoy, turnos.contarEsperaDelDia(hoy),
                salidas.countByTurnoFechaAndEstadoIn(hoy, List.of(EstadoSalida.EN_RUTA, EstadoSalida.FINALIZADA)),
                encomiendas.countByEstadoIn(List.of(EstadoEncomienda.RECIBIDA, EstadoEncomienda.ASIGNADA, EstadoEncomienda.EN_TRANSITO, EstadoEncomienda.DISPONIBLE)),
                pendientes == null ? new BigDecimal("0.00") : pendientes.setScale(2), vehiculos.countByActivoTrue());
    }
}
