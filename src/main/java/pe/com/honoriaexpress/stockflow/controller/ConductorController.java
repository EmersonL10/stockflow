package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.honoriaexpress.stockflow.entity.Conductor;
import pe.com.honoriaexpress.stockflow.service.ConductorService;

@Controller
@RequestMapping("/conductores")
@RequiredArgsConstructor
public class ConductorController {

    private final ConductorService conductorService;

    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "conductores",
                conductorService.listarTodos()
        );

        return "conductores/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "conductor",
                new Conductor()
        );

        return "conductores/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model) {

        Conductor conductor = conductorService
                .buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conductor no encontrado con id: " + id
                        )
                );

        model.addAttribute(
                "conductor",
                conductor
        );

        return "conductores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("conductor") Conductor conductor,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        /*
         * ============================================================
         * VALIDACIÓN DE DNI DUPLICADO
         * ============================================================
         */

        if (conductor.getId() == null) {

            if (conductorService.existePorDni(conductor.getDni())) {

                result.rejectValue(
                        "dni",
                        "dni.duplicado",
                        "Ya existe un conductor registrado con este DNI"
                );
            }

        } else {

            if (conductorService.existeDniEnOtroConductor(
                    conductor.getDni(),
                    conductor.getId())) {

                result.rejectValue(
                        "dni",
                        "dni.duplicado",
                        "Ya existe otro conductor registrado con este DNI"
                );
            }
        }

        /*
         * ============================================================
         * VALIDACIÓN DE LICENCIA DUPLICADA
         * ============================================================
         */

        if (conductor.getId() == null) {

            if (conductorService.existePorLicencia(
                    conductor.getLicencia())) {

                result.rejectValue(
                        "licencia",
                        "licencia.duplicada",
                        "Ya existe un conductor registrado con esta licencia"
                );
            }

        } else {

            if (conductorService.existeLicenciaEnOtroConductor(
                    conductor.getLicencia(),
                    conductor.getId())) {

                result.rejectValue(
                        "licencia",
                        "licencia.duplicada",
                        "Ya existe otro conductor registrado con esta licencia"
                );
            }
        }

        /*
         * ============================================================
         * SI HAY ERRORES, REGRESAMOS AL FORMULARIO
         * ============================================================
         */

        if (result.hasErrors()) {
            return "conductores/formulario";
        }

        /*
         * ============================================================
         * SABEMOS SI ES REGISTRO NUEVO O EDICIÓN
         * ============================================================
         */

        boolean esNuevo = conductor.getId() == null;

        conductorService.guardar(conductor);

        /*
         * ============================================================
         * MENSAJE DE ÉXITO
         * ============================================================
         */

        if (esNuevo) {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Conductor registrado correctamente."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Conductor actualizado correctamente."
            );
        }

        return "redirect:/conductores";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Conductor conductor = conductorService
                .buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conductor no encontrado con id: " + id
                        )
                );

        boolean estabaActivo = conductor.getActivo();

        conductorService.cambiarEstado(id);

        if (estabaActivo) {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Conductor inactivado correctamente."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Conductor activado correctamente."
            );
        }

        return "redirect:/conductores";
    }
}