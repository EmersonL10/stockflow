package pe.com.honoriaexpress.stockflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.exception.CajaOperacionException;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMINISTRADOR','CAJA')")
public class CajaServiceImpl implements CajaService {
    private static final Logger log = LoggerFactory.getLogger(CajaServiceImpl.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final CajaSesionRepository cajas;
    private final MovimientoCajaRepository movimientos;

    public Optional<CajaSesion> abierta() { return cajas.findByClaveActiva(1); }
    public CajaSesion buscar(Long id) { return cajas.findById(id).orElseThrow(() -> new CajaOperacionException("La caja no existe.")); }
    public List<CajaSesion> historial(int pagina) { return cajas.findAllByOrderByIdDesc(PageRequest.of(Math.max(0, pagina), 20)); }
    public List<MovimientoCaja> movimientos(Long id) { return movimientos.findByCajaIdOrderByIdDesc(id); }
    public long pagosSinCaja() { return movimientos.contarPagosSinCaja(); }

    public ResumenCaja resumir(CajaSesion caja, List<MovimientoCaja> registros) {
        BigDecimal ingresos = cero(), egresos = cero(), entradas = cero(), salidas = cero();
        for (var m : registros) {
            if (m.getTipo() == TipoMovimientoCaja.INGRESO) {
                ingresos = ingresos.add(m.getImporte());
                if (m.getMedio() == MedioPago.EFECTIVO) entradas = entradas.add(m.getImporte());
            } else {
                egresos = egresos.add(m.getImporte());
                if (m.getMedio() == MedioPago.EFECTIVO) salidas = salidas.add(m.getImporte());
            }
        }
        return new ResumenCaja(ingresos, egresos, entradas, salidas, ingresos.subtract(entradas),
                egresos.subtract(salidas), caja.getSaldoInicial().add(entradas).subtract(salidas));
    }

    @Transactional
    public CajaSesion abrir(BigDecimal saldoInicial) {
        var monto = dinero(saldoInicial, true);
        if (abierta().isPresent()) throw new CajaOperacionException("Ya hay una caja abierta. Ciérrela antes de abrir otra.");
        var caja = new CajaSesion();
        caja.setClaveActiva(1); caja.setApertura(LocalDateTime.now(LIMA)); caja.setAbiertaPor(usuario()); caja.setSaldoInicial(monto);
        // La restricción única protege también dos aperturas simultáneas sobre una tabla vacía.
        return cajas.saveAndFlush(caja);
    }

    @Transactional
    public void registrarManual(Long cajaId, TipoMovimientoCaja tipo, MedioPago medio,
                                BigDecimal importe, String concepto, String solicitud) {
        var caja = bloquear(cajaId); verificarHoy(caja);
        if (tipo == null || medio == null) throw new CajaOperacionException("Seleccione tipo y medio de pago.");
        var monto = dinero(importe, false);
        var descripcion = StringUtils.normalizeSpace(StringUtils.defaultString(concepto));
        if (descripcion.isEmpty() || descripcion.length() > 120) throw new CajaOperacionException("Indique un concepto de hasta 120 caracteres.");
        try { UUID.fromString(solicitud); }
        catch (RuntimeException ex) { throw new CajaOperacionException("Formulario inválido. Recargue Caja e intente nuevamente."); }
        if (tipo == TipoMovimientoCaja.EGRESO && medio == MedioPago.EFECTIVO
                && monto.compareTo(resumir(caja, movimientos(cajaId)).efectivoEsperado()) > 0) {
            throw new CajaOperacionException("El egreso supera el efectivo disponible en esta caja.");
        }
        guardarMovimiento(caja, null, tipo, medio, monto, descripcion, solicitud);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarCobroSalida(Salida salida, MedioPago medio) {
        var caja = cajas.bloquearAbierta().orElseThrow(() -> new CajaOperacionException("Abra una caja antes de registrar el pago de la salida."));
        verificarHoy(caja);
        if (medio == null || salida.getId() == null || salida.getEstadoCuota() != EstadoCuota.PAGADA) {
            throw new CajaOperacionException("El cobro no tiene una salida pagada y un medio válidos.");
        }
        if (movimientos.existsBySalidaId(salida.getId())) throw new CajaOperacionException("La cuota de esta salida ya está registrada en Caja.");
        guardarMovimiento(caja, salida, TipoMovimientoCaja.INGRESO, medio, dinero(salida.getCuotaAgencia(), false),
                "Cuota de agencia por salida #" + salida.getId(), "SALIDA:" + salida.getId());
    }

    @Transactional
    public void cerrar(Long id, BigDecimal contado, String observacion) {
        var caja = bloquear(id);
        BigDecimal efectivo = dinero(contado, true, "999999999999.99");
        BigDecimal esperado = resumir(caja, movimientos(id)).efectivoEsperado();
        BigDecimal diferencia = efectivo.subtract(esperado);
        String motivo = StringUtils.normalizeSpace(StringUtils.defaultString(observacion));
        if (motivo.length() > 250 || (diferencia.signum() != 0 && motivo.isEmpty())) {
            throw new CajaOperacionException("Explique la diferencia de efectivo con un motivo de hasta 250 caracteres.");
        }
        caja.setEfectivoEsperado(esperado); caja.setEfectivoContado(efectivo); caja.setDiferencia(diferencia);
        caja.setObservacionCierre(motivo); caja.setCerradaPor(usuario()); caja.setCierre(LocalDateTime.now(LIMA)); caja.setClaveActiva(null);
        cajas.saveAndFlush(caja);
        log.info("Cierre de caja registrado cajaId={}", id);
    }

    private CajaSesion bloquear(Long id) {
        var caja = cajas.bloquearPorId(id).orElseThrow(() -> new CajaOperacionException("La caja no existe."));
        if (!caja.isAbierta()) throw new CajaOperacionException("La caja ya está cerrada y no admite cambios.");
        return caja;
    }
    private void verificarHoy(CajaSesion caja) {
        if (!caja.getApertura().toLocalDate().equals(LocalDate.now(LIMA))) {
            throw new CajaOperacionException("La caja abierta es de otra fecha. Ciérrela y abra la caja de hoy antes de registrar movimientos.");
        }
    }
    private void guardarMovimiento(CajaSesion caja, Salida salida, TipoMovimientoCaja tipo, MedioPago medio,
                                   BigDecimal monto, String concepto, String solicitud) {
        var m = new MovimientoCaja();
        m.setCaja(caja); m.setSalida(salida); m.setTipo(tipo); m.setMedio(medio); m.setImporte(monto);
        m.setConcepto(concepto); m.setSolicitud(solicitud); m.setFecha(LocalDateTime.now(LIMA)); m.setRegistradoPor(usuario());
        movimientos.saveAndFlush(m);
    }
    private BigDecimal dinero(BigDecimal monto, boolean admiteCero) {
        return dinero(monto, admiteCero, "99999999.99");
    }
    private BigDecimal dinero(BigDecimal monto, boolean admiteCero, String maximo) {
        if (monto == null || monto.signum() < 0 || (!admiteCero && monto.signum() == 0)
                || monto.scale() > 2 || monto.compareTo(new BigDecimal(maximo)) > 0) {
            throw new CajaOperacionException("Use un importe " + (admiteCero ? "no negativo" : "mayor que cero") + " de hasta " + maximo + " y 2 decimales.");
        }
        return monto.setScale(2, RoundingMode.UNNECESSARY);
    }
    private String usuario() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    private BigDecimal cero() { return BigDecimal.ZERO.setScale(2); }
}
