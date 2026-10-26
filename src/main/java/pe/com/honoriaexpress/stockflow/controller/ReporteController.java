package pe.com.honoriaexpress.stockflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.com.honoriaexpress.stockflow.service.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERATIVO','CAJA')")
public class ReporteController {
    private final ReporteService reportes;
    private static final int TAMANO_PAGINA = 50;

    @GetMapping
    public String consultar(Model model, @RequestParam(defaultValue = "SALIDAS") ModuloReporte modulo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina, jakarta.servlet.http.HttpServletResponse response) {
        if (desde == null) desde = FechaOperativa.hoy();
        if (hasta == null) hasta = FechaOperativa.hoy();
        model.addAttribute("modulo", modulo); model.addAttribute("desde", desde); model.addAttribute("hasta", hasta);
        response.setHeader("Cache-Control", "no-store");
        try {
            var resultado = reportes.consultar(modulo, desde, hasta);
            int paginas = Math.max(1, (resultado.filas().size() + TAMANO_PAGINA - 1) / TAMANO_PAGINA);
            int actual = Math.max(0, Math.min(pagina, paginas - 1));
            int inicio = actual * TAMANO_PAGINA;
            model.addAttribute("resultado", resultado);
            model.addAttribute("filas", resultado.filas().subList(inicio, Math.min(inicio + TAMANO_PAGINA, resultado.filas().size())));
            model.addAttribute("pagina", actual); model.addAttribute("paginas", paginas);
        } catch (ResponseStatusException ex) {
            response.setStatus(ex.getStatusCode().value());
            model.addAttribute("error", ex.getReason()); model.addAttribute("filas", List.of());
        }
        return "reportes/lista";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(defaultValue = "SALIDAS") ModuloReporte modulo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) throws IOException {
        var resultado = reportes.consultar(modulo, desde, hasta);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .header("Content-Disposition", "attachment; filename=\"reporte-" + modulo.name().toLowerCase(java.util.Locale.ROOT)
                        + "-" + desde + "-" + hasta + ".xlsx\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportes.exportar(resultado));
    }
}
