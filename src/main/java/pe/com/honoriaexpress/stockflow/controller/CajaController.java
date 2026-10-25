package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.honoriaexpress.stockflow.service.CajaService;
import pe.com.honoriaexpress.stockflow.exception.CajaOperacionException;
import java.util.UUID;

@Controller @RequestMapping("/caja") @RequiredArgsConstructor
public class CajaController {
    private final CajaService cajas;

    @GetMapping
    public String inicio(@RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("actual", cajas.abierta().orElse(null));
        model.addAttribute("historial", cajas.historial(pagina));
        model.addAttribute("pagina", Math.max(0, pagina));
        model.addAttribute("pagosSinCaja", cajas.pagosSinCaja());
        return "caja/inicio";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            var caja = cajas.buscar(id);
            var movimientos = cajas.movimientos(id);
            model.addAttribute("caja", caja);
            model.addAttribute("movimientos", movimientos);
            model.addAttribute("resumen", cajas.resumir(caja, movimientos));
            model.addAttribute("solicitud", UUID.randomUUID().toString());
            return "caja/detalle";
        } catch (CajaOperacionException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/caja";
        }
    }

    @PostMapping("/abrir")
    public String abrir(@Valid @ModelAttribute CajaFormularios.Apertura form, BindingResult errores, RedirectAttributes flash) {
        if (invalido(errores, flash)) return "redirect:/caja";
        try {
            var caja = cajas.abrir(form.saldoInicial());
            flash.addFlashAttribute("mensaje", "Caja abierta correctamente.");
            return "redirect:/caja/" + caja.getId();
        } catch (CajaOperacionException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof CajaOperacionException ? ex.getMessage() : "Otro usuario ya abrió una caja. Actualice la pantalla.");
            return "redirect:/caja";
        }
    }

    @PostMapping("/{id}/movimientos")
    public String movimiento(@PathVariable Long id, @Valid @ModelAttribute CajaFormularios.Movimiento form,
                             BindingResult errores, RedirectAttributes flash) {
        if (invalido(errores, flash)) return "redirect:/caja/" + id;
        return ejecutar(id, flash, () -> cajas.registrarManual(id, form.tipo(), form.medio(), form.importe(), form.concepto(), form.solicitud()),
                "Movimiento registrado correctamente.");
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id, @Valid @ModelAttribute CajaFormularios.Cierre form,
                         BindingResult errores, RedirectAttributes flash) {
        // Conservar el conteo y abrir el arqueo si hay un error, para corregir sin empezar otra vez.
        flash.addFlashAttribute("mostrarCierre", true);
        flash.addFlashAttribute("contadoCierre", form.contado());
        flash.addFlashAttribute("observacionCierreEntrada", form.observacion());
        if (invalido(errores, flash)) return "redirect:/caja/" + id;
        try {
            cajas.cerrar(id, form.contado(), form.observacion());
            flash.addFlashAttribute("mensaje", "Caja cerrada. Ya puede abrir la siguiente con el efectivo inicial que corresponda.");
        } catch (CajaOperacionException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        catch (DataIntegrityViolationException ex) { flash.addFlashAttribute("error", "La caja cambió mientras cerraba. Revise el saldo actualizado antes de confirmar."); }
        return "redirect:/caja/" + id;
    }

    private boolean invalido(BindingResult errores, RedirectAttributes flash) {
        if (!errores.hasErrors()) return false;
        flash.addFlashAttribute("error", "Revise los datos: " + errores.getAllErrors().getFirst().getDefaultMessage());
        return true;
    }
    private String ejecutar(Long id, RedirectAttributes flash, Runnable accion, String mensaje) {
        try { accion.run(); flash.addFlashAttribute("mensaje", mensaje); }
        catch (CajaOperacionException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        catch (DataIntegrityViolationException ex) { flash.addFlashAttribute("error", "La operación ya fue registrada o hubo un cambio simultáneo. Actualice antes de intentar nuevamente."); }
        return "redirect:/caja/" + id;
    }
}
