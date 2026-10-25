package pe.com.honoriaexpress.stockflow.service;

import com.google.common.collect.ImmutableListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import pe.com.honoriaexpress.stockflow.entity.EstadoCuota;
import pe.com.honoriaexpress.stockflow.entity.Salida;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Misma selección para la pantalla y la exportación; no modifica los datos almacenados. */
@Service
public class ConsultaSalidasService {
    public String normalizar(String texto) {
        return StringUtils.normalizeSpace(StringUtils.defaultString(texto));
    }

    public List<Salida> filtrar(List<Salida> registros, LocalDate fecha, String ruta,
                               String placa, EstadoCuota pago, boolean todasFechas) {
        String rutaBuscada = normalizar(ruta);
        String placaBuscada = normalizar(placa).toUpperCase(Locale.ROOT);
        return registros.stream()
                .filter(s -> todasFechas || fecha.equals(s.getTurno().getFecha()))
                .filter(s -> rutaBuscada.isEmpty() || normalizar(s.getRutaMostrada()).equalsIgnoreCase(rutaBuscada))
                .filter(s -> placaBuscada.isEmpty() || normalizar(s.getTurno().getVehiculo().getPlaca())
                        .toUpperCase(Locale.ROOT).contains(placaBuscada))
                .filter(s -> pago == null || s.getEstadoCuota() == pago)
                .toList();
    }

    public ImmutableListMultimap<String, Salida> agruparPorRuta(List<Salida> registros) {
        var grupos = ImmutableListMultimap.<String, Salida>builder();
        registros.forEach(s -> grupos.put(normalizar(s.getRutaMostrada()), s));
        return grupos.build();
    }
}
