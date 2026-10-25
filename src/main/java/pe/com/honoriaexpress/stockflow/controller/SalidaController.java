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
    private final pe.com.honoriaexpress.stockflow.service.ConsultaSalidasService consultaSalidas;
    private final pe.com.honoriaexpress.stockflow.service.ExportacionSalidasService exportacionSalidas;

    @InitBinder("salida")
    public void protegerDatosFinancieros(org.springframework.web.bind.WebDataBinder binder) {
        // Solo el identificador del turno se recibe, nunca sus datos persistidos.
        binder.setAllowedFields("id", "turno.id", "rutaReal", "horaSalidaReal", "cantidadPasajeros", "estado", "observacion", "pagoAlSalir", "medioPago", "cargaConfirmada", "cargaIds");
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
        model.addAttribute("totalHistorico", registros.size());
        model.addAttribute("ultimaFecha", registros.stream().map(s -> s.getTurno().getFecha()).max(java.util.Comparator.naturalOrder()).orElse(null));
        model.addAttribute("rutas", registros.stream()
                .map(salida -> salida.getRutaMostrada())
                .distinct()
                .sorted()
                .toList());
        model.addAttribute("salidas", consultaSalidas.filtrar(registros, dia, ruta, placa, pago, todasFechas));
        model.addAttribute("resumen", salidaService.resumirDia(fecha));
        model.addAttribute("fecha", fecha);
        model.addAttribute("ruta", ruta);
        model.addAttribute("placa", placa);
        model.addAttribute("pago", pago);
        model.addAttribute("todasFechas", todasFechas);

        return "salidas/lista";
    }

    @GetMapping("/exportar")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO','CAJA')")
    public org.springframework.http.ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fecha,
            @RequestParam(defaultValue = "") String ruta,
            @RequestParam(defaultValue = "") String placa,
            @RequestParam(required = false) pe.com.honoriaexpress.stockflow.entity.EstadoCuota pago,
            @RequestParam(defaultValue = "false") boolean todasFechas) throws java.io.IOException {
        if (fecha == null) fecha = java.time.LocalDate.now(java.time.ZoneId.of("America/Lima"));
        var seleccion = consultaSalidas.filtrar(salidaService.listarTodos(), fecha, ruta, placa, pago, todasFechas);
        if (seleccion.size() > pe.com.honoriaexpress.stockflow.service.ExportacionSalidasService.MAX_FILAS) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT,
                    "Reduzca el período o aplique filtros: máximo 10000 salidas por archivo.");
        }
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"salidas-" + fecha + ".xlsx\"")
                .header("Cache-Control", "no-store")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(exportacionSalidas.exportar(seleccion));
    }

    @PostMapping("/{id}/pago")
    public String registrarPago(@PathVariable Long id,
                               @RequestParam(defaultValue = "EFECTIVO") pe.com.honoriaexpress.stockflow.entity.MedioPago medioPago,
                               RedirectAttributes flash) {
        try {
            salidaService.registrarPago(id, medioPago);
            flash.addFlashAttribute("mensaje", "Pago de cuota registrado correctamente.");
        } catch (SalidaValidationException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (pe.com.honoriaexpress.stockflow.exception.CajaOperacionException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/salidas";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model, @RequestParam(required = false) Long turnoId) {

        Salida salida = new Salida();
        salida.setEstado(EstadoSalida.EN_RUTA);
        salida.setHoraSalidaReal(java.time.LocalTime.now(java.time.ZoneId.of("America/Lima")).withSecond(0).withNano(0));

        model.addAttribute("salida", salida);

        cargarDatosFormulario(model);
        if (turnoId != null) {
            turnoService.listarColaPendiente().stream().filter(t -> t.getId().equals(turnoId)).findFirst().ifPresent(salida::setTurno);
        }

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
            if (esNuevo) salida.setId(null);
            log.warn("Conflicto de integridad al guardar una salida");
            bindingResult.reject("salida.conflicto", "No se pudo guardar. Otro usuario pudo modificar el turno o registrar su salida. Recargue y revise los datos.");
            cargarDatosFormulario(model);
            return "salidas/formulario";
        } catch (pe.com.honoriaexpress.stockflow.exception.CajaOperacionException ex) {
            // Un flush puede asignar ID antes de que Caja provoque rollback. El formulario debe seguir siendo un alta.
            if (esNuevo) salida.setId(null);
            bindingResult.reject("salida.caja", ex.getMessage());
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
        var manifiesto = new java.util.LinkedHashMap<Long, java.util.List<pe.com.honoriaexpress.stockflow.entity.Encomienda>>();
        for (var turno : turnos) manifiesto.put(turno.getId(), salidaService.encomiendasAsignadas(turno.getId()));
        model.addAttribute("manifiesto", manifiesto);
        model.addAttribute("tarifaAgencia", salidaService.getTarifaAgencia());

        model.addAttribute(
                "estados",
                EstadoSalida.values()
        );
    }
}
