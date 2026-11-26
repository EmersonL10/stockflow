package pe.com.honoriaexpress.stockflow.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class MetricasOperacionConfig {

    @Bean
    public MeterBinder metricasAgencia(JdbcTemplate jdbc) {
        return registry -> {
            Gauge.builder("stockflow.cuotas.pendientes", jdbc,
                    db -> cantidad(db, "SELECT COUNT(*) FROM salidas WHERE estado_cuota = 'PENDIENTE'"))
                    .description("Cuotas pendientes; no son ingresos recibidos").register(registry);
            Gauge.builder("stockflow.vehiculos.activos", jdbc,
                    db -> cantidad(db, "SELECT COUNT(*) FROM vehiculos WHERE activo = TRUE"))
                    .description("Unidades activas registradas").register(registry);
            Gauge.builder("stockflow.cajas.abiertas", jdbc,
                    db -> cantidad(db, "SELECT COUNT(*) FROM caja_sesiones WHERE cierre IS NULL"))
                    .description("Sesiones que requieren cierre o arqueo").register(registry);
        };
    }

    private static double cantidad(JdbcTemplate jdbc, String sql) {
        Long valor = jdbc.queryForObject(sql, Long.class);
        return valor == null ? 0 : valor.doubleValue();
    }
}
