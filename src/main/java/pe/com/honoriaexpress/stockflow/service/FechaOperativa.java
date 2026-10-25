package pe.com.honoriaexpress.stockflow.service;

import java.time.*;

/** Fecha local compartida por la operación diaria y las vistas. */
public final class FechaOperativa {
    public static final ZoneId ZONA = ZoneId.of("America/Lima");
    private FechaOperativa() { }
    public static LocalDate hoy() { return LocalDate.now(ZONA); }
}
