package pe.com.honoriaexpress.stockflow.entity;

public enum EstadoEncomienda {
    RECIBIDA("Recibida"), EN_TRANSITO("En tránsito"), DISPONIBLE("Disponible"),
    ENTREGADA("Entregada"), CANCELADA("Cancelada");

    private final String etiqueta;
    EstadoEncomienda(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
