package pe.com.honoriaexpress.stockflow.entity;

public enum EstadoSalida {
    PROGRAMADA("Programada"),
    EN_RUTA("En ruta"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String etiqueta;
    EstadoSalida(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
