package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.honoriaexpress.stockflow.repository.MovimientoCajaRepository;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadOperacionIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MovimientoCajaRepository movimientos;

    @Test void saludAnonimaNoExponeComponentesNiDetallesDeBase() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test void metricasExigenAutenticacionHttp() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERATIVO", "CAJA"})
    void metricasNoDisponiblesParaRolesDeNegocio(String rol) throws Exception {
        mvc.perform(get("/actuator/metrics").with(user("prueba").roles(rol)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"stockflow.cuotas.pendientes", "stockflow.vehiculos.activos", "stockflow.cajas.abiertas"})
    void administradorConsultaMetricasSinIdentificarPersonas(String nombre) throws Exception {
        mvc.perform(get("/actuator/metrics/" + nombre).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.measurements[0].value", greaterThanOrEqualTo(0.0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"env", "configprops", "heapdump", "logfile", "shutdown"})
    void endpointsSensiblesNoEstanExpuestos(String endpoint) throws Exception {
        mvc.perform(get("/actuator/" + endpoint).with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isNotFound());
    }

    @Test void postSinCsrfNoGeneraMovimiento() throws Exception {
        long antes = movimientos.count();
        mvc.perform(post("/salidas/15/pago").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        assertEquals(antes, movimientos.count());
    }

    @Test void csrfInvalidoNoGeneraMovimiento() throws Exception {
        long antes = movimientos.count();
        mvc.perform(post("/salidas/15/pago").with(user("admin").roles("ADMINISTRADOR"))
                .with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        assertEquals(antes, movimientos.count());
    }

    @Test void operativoNoCobraAunqueTengaTokenValido() throws Exception {
        long antes = movimientos.count();
        mvc.perform(post("/salidas/15/pago").with(user("operador").roles("OPERATIVO"))
                .with(csrf())).andExpect(status().isForbidden());
        assertEquals(antes, movimientos.count());
    }

    @Test void respuestaPermiteCorrelacionSinExponerDatos() throws Exception {
        mvc.perform(get("/login").header("X-Request-ID", "prueba-001"))
                .andExpect(status().isOk()).andExpect(header().string("X-Request-ID", "prueba-001"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
