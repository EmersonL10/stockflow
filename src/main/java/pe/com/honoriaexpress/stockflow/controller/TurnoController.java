package pe.com.honoriaexpress.stockflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.com.honoriaexpress.stockflow.entity.EstadoTurno;
import pe.com.honoriaexpress.stockflow.entity.SentidoRuta;
import pe.com.honoriaexpress.stockflow.entity.Turno;
import pe.com.honoriaexpress.stockflow.service.ConductorService;
import pe.com.honoriaexpress.stockflow.service.TurnoService;
import pe.com.honoriaexpress.stockflow.service.VehiculoService;

@Controller
@RequestMapping("/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;
    private final ConductorService conductorService;
    private final VehiculoService vehiculoService;


    /*
     * ============================================================
     * LISTAR TURNOS
     * ============================================================
     */
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "turnos",
                turnoService.listarTodos()
        );

        return "turnos/lista";
    }


    /*
     * ============================================================
     * FORMULARIO DE NUEVO TURNO
     * ============================================================
     */
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        Turno turno = new Turno();

        turno.setEstado(
                EstadoTurno.PROGRAMADO
        );

        model.addAttribute(
                "turno",
                turno
        );

        cargarCatalogos(model);

        return "turnos/formulario";
    }


    /*
     * ============================================================
     * EDITAR TURNO
     * ============================================================
     */
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model) {

        Turno turno = turnoService
                .buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Turno no encontrado con id: " + id
                        )
                );

        model.addAttribute(
                "turno",
                turno
        );

        cargarCatalogos(model);

        return "turnos/formulario";
    }


    /*
     * ============================================================
     * GUARDAR / ACTUALIZAR
     * ============================================================
     */
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("turno") Turno turno,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        /*
         * ========================================================
         * VALIDAR DUPLICIDAD DEL NÚMERO DE TURNO
         * ========================================================
         */

        if (datosBasicosValidos(turno)) {

            if (turno.getId() == null) {

                if (turnoService.existeTurno(
                        turno.getFecha(),
                        turno.getSentidoRuta(),
                        turno.getNumeroTurno())) {

                    result.rejectValue(
                            "numeroTurno",
                            "turno.duplicado",
                            "Ya existe este número de turno para la fecha y ruta seleccionadas"
                    );
                }

            } else {

                if (turnoService.existeTurnoEnOtroRegistro(
                        turno.getFecha(),
                        turno.getSentidoRuta(),
                        turno.getNumeroTurno(),
                        turno.getId())) {

                    result.rejectValue(
                            "numeroTurno",
                            "turno.duplicado",
                            "Ya existe otro turno con este número para la fecha y ruta seleccionadas"
                    );
                }
            }
        }


        /*
         * ========================================================
         * VALIDAR DISPONIBILIDAD DEL CONDUCTOR
         * ========================================================
         */

        if (turno.getFecha() != null
                && turno.getHoraProgramada() != null
                && turno.getConductor() != null
                && turno.getConductor().getId() != null) {

            if (turno.getId() == null) {

                if (turnoService.conductorOcupado(
                        turno.getFecha(),
                        turno.getHoraProgramada(),
                        turno.getConductor().getId())) {

                    result.rejectValue(
                            "conductor",
                            "conductor.ocupado",
                            "El conductor ya tiene otro turno programado en esta fecha y hora"
                    );
                }

            } else {

                if (turnoService.conductorOcupadoEnOtroTurno(
                        turno.getFecha(),
                        turno.getHoraProgramada(),
                        turno.getConductor().getId(),
                        turno.getId())) {

                    result.rejectValue(
                            "conductor",
                            "conductor.ocupado",
                            "El conductor ya tiene otro turno programado en esta fecha y hora"
                    );
                }
            }
        }


        /*
         * ========================================================
         * VALIDAR DISPONIBILIDAD DEL VEHÍCULO
         * ========================================================
         */

        if (turno.getFecha() != null
                && turno.getHoraProgramada() != null
                && turno.getVehiculo() != null
                && turno.getVehiculo().getId() != null) {

            if (turno.getId() == null) {

                if (turnoService.vehiculoOcupado(
                        turno.getFecha(),
                        turno.getHoraProgramada(),
                        turno.getVehiculo().getId())) {

                    result.rejectValue(
                            "vehiculo",
                            "vehiculo.ocupado",
                            "El vehículo ya tiene otro turno programado en esta fecha y hora"
                    );
                }

            } else {

                if (turnoService.vehiculoOcupadoEnOtroTurno(
                        turno.getFecha(),
                        turno.getHoraProgramada(),
                        turno.getVehiculo().getId(),
                        turno.getId())) {

                    result.rejectValue(
                            "vehiculo",
                            "vehiculo.ocupado",
                            "El vehículo ya tiene otro turno programado en esta fecha y hora"
                    );
                }
            }
        }


        /*
         * ========================================================
         * SI EXISTEN ERRORES
         * ========================================================
         */

        if (result.hasErrors()) {

            cargarCatalogos(model);

            return "turnos/formulario";
        }


        boolean esNuevo =
                turno.getId() == null;


        /*
         * Si por alguna razón el estado llega vacío
         * en un registro nuevo, usamos PROGRAMADO.
         */
        if (turno.getEstado() == null) {

            turno.setEstado(
                    EstadoTurno.PROGRAMADO
            );
        }


        turnoService.guardar(turno);


        /*
         * ========================================================
         * MENSAJE DE CONFIRMACIÓN
         * ========================================================
         */

        if (esNuevo) {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Turno registrado correctamente."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "Turno actualizado correctamente."
            );
        }


        return "redirect:/turnos";
    }


    /*
     * ============================================================
     * MÉTODO AUXILIAR:
     * CARGAR DATOS QUE NECESITA EL FORMULARIO
     * ============================================================
     */
    private void cargarCatalogos(Model model) {

        model.addAttribute(
                "conductores",
                conductorService.listarTodos()
        );

        model.addAttribute(
                "vehiculos",
                vehiculoService.listarTodos()
        );

        model.addAttribute(
                "sentidosRuta",
                SentidoRuta.values()
        );

        model.addAttribute(
                "estadosTurno",
                EstadoTurno.values()
        );
    }


    /*
     * ============================================================
     * MÉTODO AUXILIAR:
     * COMPROBAR QUE EXISTAN LOS DATOS NECESARIOS
     * ============================================================
     */
    private boolean datosBasicosValidos(Turno turno) {

        return turno.getFecha() != null
                && turno.getSentidoRuta() != null
                && turno.getNumeroTurno() != null;
    }
}