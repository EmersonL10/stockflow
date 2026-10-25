package pe.com.honoriaexpress.stockflow.service;

import java.util.Locale;
import pe.com.honoriaexpress.stockflow.entity.SentidoRuta;

public final class Rutas {
    private Rutas() { }
    public static String de(SentidoRuta ruta) {
        return ruta == SentidoRuta.PUCALLPA_HONORIA ? "Pucallpa - Honoria" : "Honoria - Pucallpa";
    }
    public static boolean coinciden(String a, String b) { return clave(a).equals(clave(b)); }
    private static String clave(String ruta) {
        return ruta == null ? "" : ruta.toLowerCase(Locale.ROOT).replaceAll("[\\s_→–—-]+", "");
    }
}
