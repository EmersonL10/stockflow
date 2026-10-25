package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.honoriaexpress.stockflow.entity.Vehiculo;
import pe.com.honoriaexpress.stockflow.service.VehiculoService;

@Controller
@RequestMapping("/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;


    /*
     * LISTAR VEHÍCULOS
     */
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "vehiculos",
                vehiculoService.listarTodos()
        );

        return "vehiculos/lista";
    }


    /*
     * FORMULARIO DE NUEVO VEHÍCULO
     */
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "vehiculo",
                new Vehiculo()
        );

        return "vehiculos/formulario";
    }


    /*
     * FORMULARIO DE EDICIÓN
     */
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model) {

        Vehiculo vehiculo = vehiculoService
                .buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vehículo no encontrado con id: " + id
                        )
                );

        model.addAttribute(
                "vehiculo",
                vehiculo
        );

        return "vehiculos/formulario";
    }


    /*
     * GUARDAR / ACTUALIZAR
     */
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("vehiculo") Vehiculo vehiculo,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        /*
         * Validación de placa duplicada.
         */
        if (vehiculo.getId() == null) {

            if (vehiculoService.existePorPlaca(
                    vehiculo.getPlaca())) {

                result.rejectValue(
                        "placa",
                        "placa.duplicada",
                        "Ya existe un vehículo registrado con esta placa"
                );
            }

        } else {

            if (vehiculoService.existePlacaEnOtroVehiculo(
                    vehiculo.getPlaca(),
                    vehiculo.getId())) {

                result.rejectValue(
                        "placa",
                        "placa.duplicada",
                        "Ya existe otro vehículo registrado con esta placa"
                );
            }
        }


        /*
         * Validación de TUC duplicado.
         */
        if (vehiculo.getId() == null) {

            if (vehiculoService.existePorNumeroTuc(
                    vehiculo.getNumeroTuc())) {

                result.rejectValue(
                        "numeroTuc",
                        "tuc.duplicado",
                        "Ya existe un vehículo registrado con este TUC"
                );
            }

        } else {

            if (vehiculoService.existeTucEnOtroVehiculo(
                    vehiculo.getNumeroTuc(),
                    vehiculo.getId())) {

                result.rejectValue(
                        "numeroTuc",
                        "tuc.duplicado",
                        "Ya existe otro vehículo registrado con este TUC"
                );
            }
        }


        /*
         * Si existe cualquier error,
         * regresamos al formulario.
         */
        if (result.hasErrors()) {
            return "vehiculos/formulario";
        }


        /*
         * Guardamos si se trata de un registro nuevo
         * antes de ejecutar save().
         */
        boolean esNuevo = vehiculo.getId() == null;

        vehiculoService.guardar(vehiculo);


        if (esNuevo) {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Vehículo registrado correctamente."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Vehículo actualizado correctamente."
            );
        }

        return "redirect:/vehiculos";
    }


    /*
     * ACTIVAR / INACTIVAR
     */
    @PostMapping("/estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Vehiculo vehiculo = vehiculoService
                .buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vehículo no encontrado con id: " + id
                        )
                );

        boolean estabaActivo =
                vehiculo.getActivo();

        vehiculoService.cambiarEstado(id);

        if (estabaActivo) {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Vehículo inactivado correctamente."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Vehículo activado correctamente."
            );
        }

        return "redirect:/vehiculos";
    }
}