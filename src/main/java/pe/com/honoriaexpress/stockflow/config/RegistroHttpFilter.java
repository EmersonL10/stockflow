package pe.com.honoriaexpress.stockflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistroHttpFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RegistroHttpFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recibido = request.getHeader("X-Request-ID");
        String id = recibido != null && recibido.matches("[A-Za-z0-9-]{1,64}")
                ? recibido : UUID.randomUUID().toString();
        long inicio = System.nanoTime();
        boolean fallo = false;
        MDC.put("requestId", id);
        response.setHeader("X-Request-ID", id);
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            fallo = true;
            throw ex;
        } finally {
            int estado = fallo ? 500 : response.getStatus();
            var evento = estado >= 500 ? LOG.atError() : estado >= 400 ? LOG.atWarn() : LOG.atInfo();
            // No se guardan cuerpo, parámetros, cookies, credenciales ni datos personales.
            evento.addKeyValue("event", "http_request")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("status", estado)
                    .addKeyValue("durationMs", (System.nanoTime() - inicio) / 1_000_000)
                    .log("Solicitud HTTP completada");
            MDC.remove("requestId");
        }
    }
}
