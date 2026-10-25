package pe.com.honoriaexpress.stockflow.controller;

import org.springframework.web.bind.annotation.*;
import pe.com.honoriaexpress.stockflow.service.FechaOperativa;
import java.time.LocalDate;

@ControllerAdvice
public class FechaOperativaAdvice {
    @ModelAttribute
    public void sinCache(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    @ModelAttribute("hoyOperativo")
    public LocalDate hoy() { return FechaOperativa.hoy(); }
}
