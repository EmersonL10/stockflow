package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.honoriaexpress.stockflow.dto.*;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.exception.*;
import pe.com.honoriaexpress.stockflow.service.EncomiendaService;
import java.util.UUID;

@Controller @RequestMapping("/encomiendas") @RequiredArgsConstructor
public class EncomiendaController {
    private final EncomiendaService service;

    @ModelAttribute
    public void protegerCache(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @InitBinder("form")
    public void limitarEntrada(WebDataBinder binder) {
        binder.setAllowedFields("remitente", "documentoRemitente", "telefonoRemitente", "destinatario", "documentoDestinatario",
                "telefonoDestinatario", "ruta", "descripcion", "bultos", "pesoKg", "importe", "solicitud");
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String codigo, @RequestParam(required = false) EstadoEncomienda estado,
                         @RequestParam(required = false) EstadoPagoEncomienda pago, @RequestParam(defaultValue = "0") int pagina,
                         Model model, RedirectAttributes flash) {
        try { model.addAttribute("resultado", service.consultar(codigo, estado, pago, pagina)); }
        catch (EncomiendaOperacionException ex) { flash.addFlashAttribute("error", ex.getMessage()); return "redirect:/encomiendas"; }
        model.addAttribute("codigo", codigo); model.addAttribute("estado", estado); model.addAttribute("pago", pago);
        return "encomiendas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        var form = new EncomiendaFormulario(); form.setSolicitud(UUID.randomUUID().toString());
        model.addAttribute("form", form); model.addAttribute("id", null);
        return "encomiendas/formulario";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            var e = service.buscar(id);
            model.addAttribute("e", e); model.addAttribute("eventos", service.eventos(id));
            model.addAttribute("turnos", e.getEstado() == EstadoEncomienda.RECIBIDA ? service.turnosDisponibles(e.getRuta()) : java.util.List.of());
            return "encomiendas/detalle";
        } catch (EncomiendaOperacionException ex) { flash.addFlashAttribute("error", ex.getMessage()); return "redirect:/encomiendas"; }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            var e = service.buscar(id);
            if (e.getEstado() != EstadoEncomienda.RECIBIDA || e.getEstadoPago() != EstadoPagoEncomienda.PENDIENTE) {
                flash.addFlashAttribute("error", "Solo se editan encomiendas sin despachar y pendientes de pago."); return "redirect:/encomiendas/" + id;
            }
            var f = new EncomiendaFormulario();
            f.setRemitente(e.getRemitente()); f.setDocumentoRemitente(e.getDocumentoRemitente()); f.setTelefonoRemitente(e.getTelefonoRemitente());
            f.setDestinatario(e.getDestinatario()); f.setDocumentoDestinatario(e.getDocumentoDestinatario()); f.setTelefonoDestinatario(e.getTelefonoDestinatario());
            f.setRuta(e.getRuta()); f.setDescripcion(e.getDescripcion()); f.setBultos(e.getBultos()); f.setPesoKg(e.getPesoKg()); f.setImporte(e.getImporte()); f.setSolicitud(e.getSolicitud());
            model.addAttribute("form", f); model.addAttribute("id", id); return "encomiendas/formulario";
        } catch (EncomiendaOperacionException ex) { flash.addFlashAttribute("error", ex.getMessage()); return "redirect:/encomiendas"; }
    }

    @PostMapping("/guardar")
    public String registrar(@Valid @ModelAttribute("form") EncomiendaFormulario form, BindingResult errores, Model model, RedirectAttributes flash) {
        return guardar(null, form, errores, model, flash);
    }
    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") EncomiendaFormulario form,
                             BindingResult errores, Model model, RedirectAttributes flash) {
        return guardar(id, form, errores, model, flash);
    }
    private String guardar(Long id, EncomiendaFormulario form, BindingResult errores, Model model, RedirectAttributes flash) {
        model.addAttribute("id", id);
        if (errores.hasErrors()) return "encomiendas/formulario";
        try {
            Long destino = id;
            if (id == null) destino = service.registrar(form).getId(); else service.editar(id, form);
            flash.addFlashAttribute("mensaje", id == null ? "Encomienda registrada correctamente." : "Datos de la encomienda actualizados.");
            return "redirect:/encomiendas/" + destino;
        } catch (EncomiendaOperacionException ex) { errores.reject("encomienda.invalida", ex.getMessage()); }
        catch (DataIntegrityViolationException ex) { errores.reject("encomienda.conflicto", "La solicitud ya fue registrada o hubo un cambio simultáneo. Consulte el listado antes de repetirla."); }
        return "encomiendas/formulario";
    }

    @PostMapping("/{id}/pago")
    public String pago(@PathVariable Long id, @RequestParam(defaultValue = "EFECTIVO") MedioPago medio, RedirectAttributes flash) {
        return ejecutar(id, flash, () -> service.cobrar(id, medio), "Cobro registrado y vinculado a Caja.");
    }
    @PostMapping("/{id}/asignar")
    public String asignar(@PathVariable Long id, @RequestParam(required = false) Long turnoId, RedirectAttributes flash) {
        return ejecutar(id, flash, () -> service.asignar(id, turnoId), "Vehículo y conductor asignados. Pendiente de salida.");
    }
    @PostMapping("/{id}/retirar-asignacion")
    public String retirar(@PathVariable Long id, RedirectAttributes flash) {
        return ejecutar(id, flash, () -> service.retirarAsignacion(id), "Asignación retirada. Puede elegir otro transporte.");
    }
    @PostMapping("/{id}/despachar")
    public String despachar(@PathVariable Long id, @RequestParam(required = false) Long salidaId, RedirectAttributes flash) {
        return ejecutar(id, flash, () -> service.despachar(id, salidaId), "Despacho registrado. La encomienda está en tránsito.");
    }
    @PostMapping("/{id}/disponible")
    public String disponible(@PathVariable Long id, RedirectAttributes flash) {
        return ejecutar(id, flash, () -> service.recibirDestino(id), "Llegada confirmada. Disponible para entrega.");
    }
    @PostMapping("/{id}/entregar")
    public String entregar(@PathVariable Long id, @Valid @ModelAttribute EntregaEncomiendaFormulario form,
                           BindingResult errores, RedirectAttributes flash) {
        if (errores.hasErrors()) { flash.addFlashAttribute("error", "Complete el nombre y documento de quien recibe."); return "redirect:/encomiendas/" + id; }
        return ejecutar(id, flash, () -> service.entregar(id, form), "Entrega registrada correctamente.");
    }
    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(defaultValue = "") String motivo, RedirectAttributes flash) {
        return ejecutar(id, flash, () -> service.cancelar(id, motivo), "Encomienda cancelada sin generar cobro.");
    }
    private String ejecutar(Long id, RedirectAttributes flash, Runnable accion, String mensaje) {
        try { accion.run(); flash.addFlashAttribute("mensaje", mensaje); }
        catch (EncomiendaOperacionException | CajaOperacionException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        catch (DataIntegrityViolationException ex) { flash.addFlashAttribute("error", "La operación ya fue registrada o hubo un cambio simultáneo. Actualice el detalle."); }
        return "redirect:/encomiendas/" + id;
    }
}
