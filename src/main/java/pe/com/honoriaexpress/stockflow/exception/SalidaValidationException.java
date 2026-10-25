package pe.com.honoriaexpress.stockflow.exception;

public class SalidaValidationException extends RuntimeException {
    private final String campo;

    public SalidaValidationException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
