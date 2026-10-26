package pe.com.honoriaexpress.stockflow.service;

public enum ModuloReporte {
    SALIDAS("Salidas"), ENCOMIENDAS("Encomiendas"), CAJA("Caja");

    private final String etiqueta;

    ModuloReporte(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
