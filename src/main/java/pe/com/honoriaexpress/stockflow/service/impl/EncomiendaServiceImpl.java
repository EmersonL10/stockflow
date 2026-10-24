package pe.com.honoriaexpress.stockflow.service.impl;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.honoriaexpress.stockflow.dto.*;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.exception.EncomiendaOperacionException;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO','CAJA')")
public class EncomiendaServiceImpl implements EncomiendaService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final EncomiendaRepository encomiendas;
    private final EncomiendaEventoRepository eventos;
    private final SalidaRepository salidas;
    private final CajaService caja;
    private final Validator validator;

    public Page<Encomienda> consultar(String codigo, EstadoEncomienda estado, EstadoPagoEncomienda pago, int pagina) {
        String busqueda = texto(codigo).toUpperCase(Locale.ROOT);
        if (busqueda.length() > 30) throw new EncomiendaOperacionException("El código de búsqueda no puede superar 30 caracteres.");
        Specification<Encomienda> filtro = (root, query, cb) -> {
            var condiciones = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!busqueda.isEmpty()) {
                String escapado = busqueda.replace("!", "!!").replace("%", "!%").replace("_", "!_");
                condiciones.add(cb.like(cb.upper(root.get("codigo")), "%" + escapado + "%", '!'));
            }
            if (estado != null) condiciones.add(cb.equal(root.get("estado"), estado));
            if (pago != null) condiciones.add(cb.equal(root.get("estadoPago"), pago));
            return cb.and(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return encomiendas.findAll(filtro, PageRequest.of(Math.max(0, pagina), 20, Sort.by(Sort.Direction.DESC, "id")));
    }

    public Encomienda buscar(Long id) {
        return encomiendas.findById(id).orElseThrow(() -> new EncomiendaOperacionException("La encomienda no existe."));
    }
    public List<EncomiendaEvento> eventos(Long id) { return eventos.findByEncomiendaIdOrderByIdAsc(id); }
    public List<Salida> salidasDisponibles(String ruta) {
        return salidas.findByEstadoAndTurnoFechaOrderByIdDesc(EstadoSalida.EN_RUTA, LocalDate.now(LIMA)).stream()
                .filter(s -> texto(s.getRutaMostrada()).equalsIgnoreCase(texto(ruta))).toList();
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO')")
    public Encomienda registrar(EncomiendaFormulario datos) {
        validar(datos);
        var e = new Encomienda(); copiar(datos, e);
        e.setSolicitud(datos.getSolicitud()); e.setRegistradaEn(LocalDateTime.now(LIMA));
        e.setEstado(EstadoEncomienda.RECIBIDA); e.setEstadoPago(EstadoPagoEncomienda.PENDIENTE);
        encomiendas.saveAndFlush(e);
        e.setCodigo("ENC-%08d".formatted(e.getId()));
        encomiendas.saveAndFlush(e);
        evento(e, "REGISTRO", "Envío recibido en agencia. Pendiente de cobro y despacho.");
        return e;
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO')")
    public void editar(Long id, EncomiendaFormulario datos) {
        var e = bloquear(id);
        if (e.getEstado() != EstadoEncomienda.RECIBIDA || e.getEstadoPago() != EstadoPagoEncomienda.PENDIENTE) {
            throw new EncomiendaOperacionException("Solo se editan encomiendas recibidas, sin despachar y pendientes de pago.");
        }
        validar(datos); copiar(datos, e); encomiendas.saveAndFlush(e);
        evento(e, "EDICION", "Se actualizaron los datos del envío antes del cobro y despacho.");
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAJA')")
    public void cobrar(Long id, MedioPago medio) {
        var e = bloquear(id);
        if (e.getEstadoPago() != EstadoPagoEncomienda.PENDIENTE || e.getEstado() == EstadoEncomienda.CANCELADA) {
            throw new EncomiendaOperacionException("La encomienda no tiene un pago pendiente.");
        }
        if (medio == null) throw new EncomiendaOperacionException("Seleccione el medio de pago.");
        e.setEstadoPago(EstadoPagoEncomienda.PAGADA); e.setFechaPago(LocalDateTime.now(LIMA)); e.setPagoRegistradoPor(usuario());
        encomiendas.saveAndFlush(e);
        caja.registrarCobroEncomienda(e, medio);
        evento(e, "PAGO", "Pago completo registrado por " + medio + " y vinculado a Caja.");
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO')")
    public void despachar(Long id, Long salidaId) {
        if (salidaId == null) throw new EncomiendaOperacionException("Seleccione una salida de la misma ruta.");
        // El orden salida -> encomienda coordina el despacho con una posible cancelación de la salida.
        var salida = salidas.buscarParaCobro(salidaId).orElseThrow(() -> new EncomiendaOperacionException("La salida no existe."));
        var e = bloquear(id);
        if (e.getEstado() != EstadoEncomienda.RECIBIDA) throw new EncomiendaOperacionException("Solo puede despachar un envío recibido y sin salida asignada.");
        if (salida.getEstado() != EstadoSalida.EN_RUTA || !salida.getTurno().getFecha().equals(LocalDate.now(LIMA))
                || !texto(salida.getRutaMostrada()).equalsIgnoreCase(texto(e.getRuta()))) {
            throw new EncomiendaOperacionException("Seleccione una salida en ruta de hoy que coincida con la ruta del envío.");
        }
        e.setSalida(salida); e.setEstado(EstadoEncomienda.EN_TRANSITO); encomiendas.saveAndFlush(e);
        evento(e, "DESPACHO", "Encomienda despachada en la salida #" + salida.getId() + ".");
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO')")
    public void recibirDestino(Long id) {
        var e = bloquear(id);
        if (e.getEstado() != EstadoEncomienda.EN_TRANSITO) throw new EncomiendaOperacionException("El envío debe estar en tránsito para confirmar su llegada.");
        e.setEstado(EstadoEncomienda.DISPONIBLE); encomiendas.saveAndFlush(e);
        evento(e, "RECEPCION_DESTINO", "Llegada a destino confirmada. Disponible para el destinatario.");
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO')")
    public void entregar(Long id, EntregaEncomiendaFormulario datos) {
        var errores = validator.validate(datos);
        if (!errores.isEmpty()) throw new EncomiendaOperacionException("Indique nombre y documento válidos de quien recibe.");
        var e = bloquear(id);
        if (e.getEstado() != EstadoEncomienda.DISPONIBLE || e.getEstadoPago() != EstadoPagoEncomienda.PAGADA) {
            throw new EncomiendaOperacionException("Para entregar debe estar disponible en destino y completamente pagada.");
        }
        if (!e.getDocumentoDestinatario().equalsIgnoreCase(texto(datos.documento()))) {
            throw new EncomiendaOperacionException("El documento de quien recibe debe coincidir con el destinatario registrado.");
        }
        e.setEstado(EstadoEncomienda.ENTREGADA); e.setEntregadaEn(LocalDateTime.now(LIMA));
        e.setRecibidaPor(texto(datos.receptor())); e.setDocumentoReceptor(texto(datos.documento()).toUpperCase(Locale.ROOT));
        encomiendas.saveAndFlush(e);
        evento(e, "ENTREGA", "Entrega al destinatario confirmada por el operador.");
    }

    @Transactional @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO')")
    public void cancelar(Long id, String motivo) {
        var e = bloquear(id); String detalle = texto(motivo);
        if (e.getEstado() != EstadoEncomienda.RECIBIDA || e.getEstadoPago() != EstadoPagoEncomienda.PENDIENTE) {
            throw new EncomiendaOperacionException("Solo puede cancelar un envío sin despachar y sin cobrar. No hay devolución automática.");
        }
        if (detalle.isEmpty() || detalle.length() > 250) throw new EncomiendaOperacionException("Indique el motivo de cancelación, máximo 250 caracteres.");
        e.setEstado(EstadoEncomienda.CANCELADA); e.setEstadoPago(EstadoPagoEncomienda.ANULADA);
        encomiendas.saveAndFlush(e); evento(e, "CANCELACION", detalle);
    }

    private Encomienda bloquear(Long id) { return encomiendas.bloquear(id).orElseThrow(() -> new EncomiendaOperacionException("La encomienda no existe.")); }
    private String texto(String valor) { return StringUtils.normalizeSpace(StringUtils.defaultString(valor)); }
    private String usuario() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    private void evento(Encomienda e, String accion, String detalle) {
        var evento = new EncomiendaEvento(); evento.setEncomienda(e); evento.setAccion(accion);
        evento.setDetalle(detalle); evento.setFecha(LocalDateTime.now(LIMA)); evento.setUsuario(usuario()); eventos.saveAndFlush(evento);
    }
    private void validar(EncomiendaFormulario datos) {
        var errores = validator.validate(datos);
        if (!errores.isEmpty()) {
            var error = errores.iterator().next();
            throw new EncomiendaOperacionException("Revise " + error.getPropertyPath() + ": " + error.getMessage());
        }
        for (String telefono : List.of(datos.getTelefonoRemitente(), datos.getTelefonoDestinatario())) {
            int digitos = StringUtils.getDigits(telefono).length();
            if (digitos < 7 || digitos > 15) throw new EncomiendaOperacionException("Los teléfonos deben contener entre 7 y 15 dígitos.");
        }
        if (texto(datos.getRemitente()).isEmpty() || texto(datos.getDestinatario()).isEmpty()
                || texto(datos.getRuta()).isEmpty() || texto(datos.getDescripcion()).isEmpty()) {
            throw new EncomiendaOperacionException("Complete los nombres, la ruta y la descripción.");
        }
    }
    private void copiar(EncomiendaFormulario f, Encomienda e) {
        e.setRemitente(texto(f.getRemitente())); e.setDocumentoRemitente(texto(f.getDocumentoRemitente()).toUpperCase(Locale.ROOT));
        e.setTelefonoRemitente(texto(f.getTelefonoRemitente())); e.setDestinatario(texto(f.getDestinatario()));
        e.setDocumentoDestinatario(texto(f.getDocumentoDestinatario()).toUpperCase(Locale.ROOT)); e.setTelefonoDestinatario(texto(f.getTelefonoDestinatario()));
        e.setRuta(texto(f.getRuta())); e.setDescripcion(texto(f.getDescripcion())); e.setBultos(f.getBultos()); e.setPesoKg(f.getPesoKg());
        e.setImporte(f.getImporte().setScale(2, java.math.RoundingMode.UNNECESSARY));
    }
}
