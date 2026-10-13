package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.honoriaexpress.stockflow.entity.EstadoSalida;
import pe.com.honoriaexpress.stockflow.entity.Salida;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.honoriaexpress.stockflow.exception.SalidaValidationException;
import pe.com.honoriaexpress.stockflow.service.SalidaService;
import pe.com.honoriaexpress.stockflow.service.TurnoService;

@Controller
@RequestMapping("/salidas")
@RequiredArgsConstructor
public class SalidaController {
    private static final Logger log = LoggerFactory.getLogger(SalidaController.class);

    private final SalidaService salidaService;
    private final TurnoService turnoService;

    @InitBinder("salida")
    public void protegerDatosFinancieros(org.springframework.web.bind.WebDataBinder binder) {
        // Solo el identificador del turno se recibe, nunca sus datos persistidos.
        binder.setAllowedFields("id", "turno.id", "rutaReal", "horaSalidaReal", "cantidadPasajeros", "estado", "observacion", "pagoAlSalir");
        binder.setDisallowedFields("cuotaAgencia", "estadoCuota", "montoRecaudado");
    }

    @GetMapping
    public String listar(Model model,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
            @RequestParam(defaultValue = "") String ruta,
            @RequestParam(defaultValue = "") String placa,
            @RequestParam(required = false) pe.com.honoriaexpress.stockflow.entity.EstadoCuota pago,
            @RequestParam(defaultValue = "false") boolean todasFechas) {
        if (fecha == null) fecha = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        final var dia = fecha;
        var registros = salidaService.listarTodos();
        model.addAttribute("rutas", registros.stream().map(Salida::getRutaMostrada).distinct().sorted().toList());
        model.addAttribute("salidas", registros.stream()
                .filter(s -> todasFechas || dia.equals(s.getTurno().getFecha()))
                .filter(s -> ruta.isBlank() || s.getRutaMostrada().equalsIgnoreCase(ruta.strip()))
                .filter(s -> placa.isBlank() || s.getTurno().getVehiculo().getPlaca().toUpperCase(java.util.Locale.ROOT).contains(placa.strip().toUpperCase(java.util.Locale.ROOT)))
                .filter(s -> pago == null || s.getEstadoCuota() == pago).toList());
        model.addAttribute("resumen", salidaService.resumirDia(fecha));
        model.addAttribute("fecha", fecha);
        model.addAttribute("ruta", ruta);
        model.addAttribute("placa", placa);
        model.addAttribute("pago", pago);
        model.addAttribute("todasFechas", todasFechas);

        return "salidas/lista";
    }

    @PostMapping("/{id}/pago")
    public String registrarPago(@PathVariable Long id, RedirectAttributes flash) {
        try {
            salidaService.registrarPago(id);
            flash.addFlashAttribute("mensaje", "Pago de cuota registrado correctamente.");
        } catch (SalidaValidationException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/salidas";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        Salida salida = new Salida();
        salida.setEstado(EstadoSalida.EN_RUTA);
        salida.setHoraSalidaReal(java.time.LocalTime.now(java.time.ZoneId.of("America/Lima")).withSecond(0).withNano(0));

        model.addAttribute("salida", salida);

        cargarDatosFormulario(model);

        return "salidas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        Salida salida = salidaService.buscarPorId(id).orElse(null);

        if (salida == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La salida solicitada no existe."
            );

            return "redirect:/salidas";
        }

        model.addAttribute("salida", salida);

        cargarDatosFormulario(model);

        return "salidas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("salida") Salida salida,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (bindingResult.hasErrors()) {

            cargarDatosFormulario(model);

            return "salidas/formulario";
        }

        boolean esNuevo = salida.getId() == null;
        try {
            salidaService.guardar(salida);
        } catch (SalidaValidationException ex) {
            bindingResult.rejectValue(ex.getCampo(), "salida.invalida", ex.getMessage());
            cargarDatosFormulario(model);
            return "salidas/formulario";
        } catch (DataIntegrityViolationException ex) {
            log.warn("Conflicto de integridad al guardar una salida");
            bindingResult.reject("salida.conflicto", "No se pudo guardar. Otro usuario pudo modificar el turno o registrar su salida. Recargue y revise los datos.");
            cargarDatosFormulario(model);
            return "salidas/formulario";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esNuevo
                        ? "Salida registrada correctamente."
                        : "Salida actualizada correctamente."
        );

        return "redirect:/salidas";
    }

    private void cargarDatosFormulario(Model model) {

        var turnos = new java.util.ArrayList<>(turnoService.listarColaPendiente());
        Salida actual = (Salida) model.getAttribute("salida");
        if (actual != null && actual.getId() != null) {
            salidaService.buscarPorId(actual.getId()).ifPresent(guardada -> {
                if (turnos.stream().noneMatch(t -> t.getId().equals(guardada.getTurno().getId()))) {
                    turnos.add(guardada.getTurno());
                }
            });
        }
        model.addAttribute("turnos", turnos);
        model.addAttribute("tarifaAgencia", salidaService.getTarifaAgencia());

        model.addAttribute(
                "estados",
                EstadoSalida.values()
        );
    }
}
