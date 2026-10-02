package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.com.honoriaexpress.stockflow.entity.Conductor;
import pe.com.honoriaexpress.stockflow.service.ConductorService;

@Controller
@RequestMapping("/conductores")
@RequiredArgsConstructor
public class ConductorController {

    private final ConductorService conductorService;


    /*
     * LISTAR CONDUCTORES
     */
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "conductores",
                conductorService.listarTodos()
        );

        return "conductores/lista";
    }


    /*
     * MOSTRAR FORMULARIO PARA REGISTRAR
     */
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "conductor",
                new Conductor()
        );

        return "conductores/formulario";
    }


    /*
     * MOSTRAR FORMULARIO PARA EDITAR
     */
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


    /*
     * GUARDAR O ACTUALIZAR CONDUCTOR
     */
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("conductor") Conductor conductor,
            BindingResult result) {

        /*
         * ============================================================
         * 1. VALIDAR DNI DUPLICADO
         * ============================================================
         */

        if (conductor.getId() == null) {

            /*
             * Es un conductor NUEVO.
             *
             * Entonces ningún otro registro debe tener el mismo DNI.
             */
            if (conductorService.existePorDni(conductor.getDni())) {

                result.rejectValue(
                        "dni",
                        "dni.duplicado",
                        "Ya existe un conductor registrado con este DNI"
                );
            }

        } else {

            /*
             * Es una EDICIÓN.
             *
             * Buscamos si el DNI pertenece a OTRO conductor,
             * ignorando el registro que estamos editando.
             */
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
         * 2. VALIDAR LICENCIA DUPLICADA
         * ============================================================
         */

        if (conductor.getId() == null) {

            /*
             * Registro nuevo.
             */
            if (conductorService.existePorLicencia(
                    conductor.getLicencia())) {

                result.rejectValue(
                        "licencia",
                        "licencia.duplicada",
                        "Ya existe un conductor registrado con esta licencia"
                );
            }

        } else {

            /*
             * Edición.
             */
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
         * 3. SI EXISTE ALGÚN ERROR, VOLVER AL FORMULARIO
         * ============================================================
         */

        if (result.hasErrors()) {
            return "conductores/formulario";
        }


        /*
         * ============================================================
         * 4. SI TODO ESTÁ CORRECTO, GUARDAR
         * ============================================================
         */

        conductorService.guardar(conductor);

        return "redirect:/conductores";
    }


    /*
     * CAMBIAR ESTADO DEL CONDUCTOR
     */
    @PostMapping("/estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id) {

        conductorService.cambiarEstado(id);

        return "redirect:/conductores";
    }
}