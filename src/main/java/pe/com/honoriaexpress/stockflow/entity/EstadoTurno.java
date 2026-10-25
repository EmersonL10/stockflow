package pe.com.honoriaexpress.stockflow.entity;

public enum EstadoTurno {

    PROGRAMADO("Programado"),
    EN_ESPERA("En espera"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado");

    private final String etiqueta;
    EstadoTurno(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
