package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.com.honoriaexpress.stockflow.entity.*;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles="ADMINISTRADOR")
class SalidasIntegrationTests {
    @Autowired SalidaService service;
    @Autowired TurnoService turnoService;
    @Autowired SalidaRepository salidas;
    @Autowired TurnoRepository turnos;
    @Autowired VehiculoRepository vehiculos;
    @Autowired ConductorRepository conductores;
    @Autowired MockMvc mvc;
    @Autowired CajaService cajaService;
    @Autowired CajaSesionRepository cajas;
    @Autowired MovimientoCajaRepository movimientosCaja;
    @Autowired EncomiendaRepository encomiendas;
    @Autowired EncomiendaEventoRepository eventosEncomienda;
    Turno turno;

    @BeforeEach void preparar() {
        eventosEncomienda.deleteAll(); movimientosCaja.deleteAll(); encomiendas.deleteAll(); cajas.deleteAll();
        salidas.deleteAll(); turnos.deleteAll(); vehiculos.deleteAll(); conductores.deleteAll();
        cajaService.abrir(java.math.BigDecimal.ZERO);
        var conductor=conductores.saveAndFlush(Conductor.builder().dni("00000001").nombres("Prueba")
                .apellidos("Integracion").licencia("TEST-LIC").build());
        var vehiculo=vehiculos.saveAndFlush(Vehiculo.builder().placa("TEST-001").marca("PRUEBA")
                .modelo("PRUEBA").anio(2020).color("BLANCO").capacidadPasajeros(5).numeroTuc("TEST-TUC").build());
        turno=turnos.saveAndFlush(Turno.builder().fecha(LocalDate.now(ZoneId.of("America/Lima")))
                .numeroTurno(1).sentidoRuta(SentidoRuta.PUCALLPA_HONORIA).horaProgramada(LocalTime.of(8,0))
                .conductor(conductor).vehiculo(vehiculo).build());
    }

    Salida nueva(boolean pagada) {
        return Salida.builder().turno(Turno.builder().id(turno.getId()).build())
                .horaSalidaReal(LocalTime.of(8,30)).cantidadPasajeros(4).pagoAlSalir(pagada).build();
    }

    @Test void flujoPagoEdicionResumenEHistorial() throws Exception {
        var s=service.guardar(nueva(false));
        assertEquals(EstadoCuota.PENDIENTE,salidas.findById(s.getId()).orElseThrow().getEstadoCuota());
        service.registrarPago(s.getId());
        var editar=nueva(false); editar.setId(s.getId()); editar.setEstado(EstadoSalida.FINALIZADA);
        service.guardar(editar);
        var guardada=salidas.findById(s.getId()).orElseThrow();
        assertEquals(EstadoCuota.PAGADA,guardada.getEstadoCuota());
        assertNotNull(guardada.getFechaPago());
        assertEquals(new java.math.BigDecimal("10.00"),service.resumirDia(turno.getFecha()).cobradoHoy());
        assertThrows(RuntimeException.class,()->turnoService.guardar(turno));
        mvc.perform(get("/salidas")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Cobrado durante este día")));
        mvc.perform(get("/salidas/nuevo")).andExpect(status().isOk());
        mvc.perform(get("/salidas/editar/"+s.getId())).andExpect(status().isOk());
        mvc.perform(get("/turnos")).andExpect(status().isOk());
    }

    @Test void altaHttpIgnoraCuotaManipulada() throws Exception {
        mvc.perform(post("/salidas/guardar").with(csrf()).param("turno.id",turno.getId().toString())
                .param("horaSalidaReal","08:30").param("cantidadPasajeros","4").param("estado","EN_RUTA")
                .param("pagoAlSalir","true").param("cuotaAgencia","0.01").param("estadoCuota","ANULADA"))
                .andExpect(status().is3xxRedirection());
        var s=salidas.findAll().getFirst();
        assertEquals(new java.math.BigDecimal("10.00"),s.getCuotaAgencia());
        assertEquals(EstadoCuota.PAGADA,s.getEstadoCuota());
    }

    @Test void seguridadYCsrF() throws Exception {
        mvc.perform(post("/salidas/guardar")).andExpect(status().isForbidden());
        mvc.perform(post("/salidas/1/pago").with(csrf()).with(user("operador").roles("OPERATIVO"))).andExpect(status().isForbidden());
        mvc.perform(get("/salidas/nuevo").with(user("cajero").roles("CAJA"))).andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test void excelHttpRespetaFiltrosPermisosYDatosNumericos() throws Exception {
        service.guardar(nueva(false));
        var resultado = mvc.perform(get("/salidas/exportar").param("fecha", turno.getFecha().toString())
                        .param("placa", " test-001 ").param("pago", "PENDIENTE")
                        .with(user("cajero").roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();
        try (var libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))) {
            assertEquals(2, libro.getSheet("Salidas").getPhysicalNumberOfRows());
            assertEquals(10.0, libro.getSheet("Salidas").getRow(1).getCell(7).getNumericCellValue());
        }
        mvc.perform(get("/salidas/exportar").with(user("sinpermiso").roles("INVITADO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/salidas/exportar").with(anonymous())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/salidas/exportar").param("pago", "INVALIDO")).andExpect(status().isBadRequest());
        mvc.perform(get("/salidas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Exportar a Excel")));
    }

    @Test void dosAltasSimultaneasSoloCreanUnaSalida() throws Exception {
        int exitos=simultaneos(()->service.guardar(nueva(false)));
        assertEquals(1,exitos);
        assertEquals(1,salidas.count());
    }

    @Test void dosCobrosSimultaneosSoloRegistranUno() throws Exception {
        var s=service.guardar(nueva(false));
        int exitos=simultaneos(()->{service.registrarPago(s.getId());return null;});
        assertEquals(1,exitos);
        assertEquals(new java.math.BigDecimal("10.00"),service.resumirDia(turno.getFecha()).cobradoHoy());
    }

    int simultaneos(Callable<?> accion) throws Exception {
        var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados=new ArrayList<>();
            for(int i=0;i<2;i++) resultados.add(pool.submit(()->{
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        "pruebas", "", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
                inicio.await();
                try {accion.call(); return true;} catch(RuntimeException ex){return false;}
                finally {SecurityContextHolder.clearContext();}
            }));
            inicio.countDown();
            int exitos=0; for(var r:resultados) if(r.get(20,TimeUnit.SECONDS)) exitos++;
            return exitos;
        }
    }
}
