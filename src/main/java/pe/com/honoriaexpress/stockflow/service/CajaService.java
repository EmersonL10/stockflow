package pe.com.honoriaexpress.stockflow.service;

import pe.com.honoriaexpress.stockflow.entity.*;
import java.math.BigDecimal;
import java.util.*;

public interface CajaService {
    Optional<CajaSesion> abierta();
    CajaSesion buscar(Long id);
    List<CajaSesion> historial(int pagina);
    List<MovimientoCaja> movimientos(Long id);
    ResumenCaja resumir(CajaSesion caja, List<MovimientoCaja> movimientos);
    long pagosSinCaja();
    CajaSesion abrir(BigDecimal saldoInicial);
    void registrarManual(Long cajaId, TipoMovimientoCaja tipo, MedioPago medio,
                         BigDecimal importe, String concepto, String solicitud);
    void registrarCobroSalida(Salida salida, MedioPago medio);
    void registrarCobroEncomienda(Encomienda encomienda, MedioPago medio);
    void cerrar(Long id, BigDecimal contado, String observacion);
}
