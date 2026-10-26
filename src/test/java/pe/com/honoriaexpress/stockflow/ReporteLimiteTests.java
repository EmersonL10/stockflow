package pe.com.honoriaexpress.stockflow;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.com.honoriaexpress.stockflow.entity.Salida;
import pe.com.honoriaexpress.stockflow.repository.*;
import pe.com.honoriaexpress.stockflow.service.*;
import java.time.LocalDate;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReporteLimiteTests {
    @Test void rechazaExcesoSinConstruirUnReporteParcial() {
        var salidas = mock(SalidaRepository.class);
        when(salidas.findByTurnoFechaBetweenOrderByTurnoFechaDescIdDesc(any(), any(), any()))
                .thenReturn(Collections.nCopies(10001, new Salida()));
        var servicio = new ReporteService(salidas, mock(EncomiendaRepository.class), mock(MovimientoCajaRepository.class));
        var error = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar(ModuloReporte.SALIDAS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        assertEquals(422, error.getStatusCode().value());
    }
}
