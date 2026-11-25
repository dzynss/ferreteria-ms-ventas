package cl.duoc.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Excepción base de negocio: los services la lanzan (nunca devuelven ResponseEntity)
 * y el ManejadorErrores la traduce al JSON de error del contrato (C4).
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getDetalles() {
        return null;
    }
}
