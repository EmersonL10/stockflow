package pe.com.honoriaexpress.stockflow.entity;

public enum EstadoPagoEncomienda {
    PENDIENTE("Pendiente"), PAGADA("Pagado"), ANULADA("Anulado");

    private final String etiqueta;
    EstadoPagoEncomienda(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
}
