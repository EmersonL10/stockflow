package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import pe.com.honoriaexpress.stockflow.controller.SalidaController;
import pe.com.honoriaexpress.stockflow.entity.Salida;
import pe.com.honoriaexpress.stockflow.service.SalidaService;
import pe.com.honoriaexpress.stockflow.service.TurnoService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalidaControllerTests {
    @Test void creacionConservaMensajeAunqueSaveAsigneId() {
        var service = mock(SalidaService.class);
        var controller = new SalidaController(service, mock(TurnoService.class),
                mock(pe.com.honoriaexpress.stockflow.service.ConsultaSalidasService.class),
                mock(pe.com.honoriaexpress.stockflow.service.ExportacionSalidasService.class));
        var salida = new Salida();
        when(service.guardar(salida)).thenAnswer(invocation -> { salida.setId(1L); return salida; });
        var flash = new RedirectAttributesModelMap();
        assertEquals("redirect:/salidas", controller.guardar(salida,
                new BeanPropertyBindingResult(salida, "salida"), new ExtendedModelMap(), flash));
        assertEquals("Salida registrada correctamente.", flash.getFlashAttributes().get("mensaje"));
    }

    @Test void conflictoDePersistenciaVuelveAlFormularioSinMensajeTecnico() {
        var service = mock(SalidaService.class);
        var controller = new SalidaController(service, mock(TurnoService.class),
                mock(pe.com.honoriaexpress.stockflow.service.ConsultaSalidasService.class),
                mock(pe.com.honoriaexpress.stockflow.service.ExportacionSalidasService.class));
        var salida = new Salida();
        when(service.guardar(salida)).thenThrow(new DataIntegrityViolationException("SQL interno"));
        var errors = new BeanPropertyBindingResult(salida, "salida");
        var model = new ExtendedModelMap();
        assertEquals("salidas/formulario", controller.guardar(salida, errors, model,
                new RedirectAttributesModelMap()));
        assertTrue(errors.hasGlobalErrors());
        assertFalse(errors.getGlobalError().getDefaultMessage().contains("SQL interno"));
        assertTrue(model.containsAttribute("turnos"));
    }
}
