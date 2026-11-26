package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import pe.com.honoriaexpress.stockflow.controller.CajaController;
import pe.com.honoriaexpress.stockflow.controller.CajaFormularios;
import pe.com.honoriaexpress.stockflow.service.CajaService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CajaControllerTests {
    @Test void cierreBloqueadoConservaConteoYMuestraReintentoSinExponerElErrorInterno() {
        var servicio = mock(CajaService.class);
        var form = new CajaFormularios.Cierre(new BigDecimal("25.00"), "Conteo revisado");
        doThrow(new CannotAcquireLockException("SQL interno")).when(servicio)
                .cerrar(1L, form.contado(), form.observacion());
        var flash = new RedirectAttributesModelMap();
        var destino = new CajaController(servicio).cerrar(1L, form,
                new BeanPropertyBindingResult(form, "cierre"), flash);
        assertEquals("redirect:/caja/1", destino);
        assertEquals(form.contado(), flash.getFlashAttributes().get("contadoCierre"));
        assertEquals(form.observacion(), flash.getFlashAttributes().get("observacionCierreEntrada"));
        assertEquals(true, flash.getFlashAttributes().get("mostrarCierre"));
        String mensaje = (String) flash.getFlashAttributes().get("error");
        assertNotNull(mensaje); assertTrue(mensaje.contains("vuelva a confirmar"));
        assertFalse(mensaje.contains("SQL interno"));
        assertFalse(flash.getFlashAttributes().containsKey("mensaje"));
    }
}
