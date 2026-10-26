package pe.com.honoriaexpress.stockflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@lombok.RequiredArgsConstructor
public class HomeController {
    private final pe.com.honoriaexpress.stockflow.service.InicioService inicio;

    @GetMapping("/")
    public String home(org.springframework.ui.Model model) {
        model.addAttribute("resumen", inicio.consultar());
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRADOR") || a.getAuthority().equals("ROLE_CAJA"))) {
            model.addAttribute("ingresosHoy", inicio.ingresosHoy());
        }
        return "home";
    }
}
