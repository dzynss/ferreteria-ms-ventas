package cl.duoc.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 400: regla de formato u obligatoriedad que Bean Validation no cubre.
 * Lleva el nombre del campo para que el front pueda pintar el error bajo el input correcto.
 */
public class SolicitudInvalidaException extends NegocioException {

    private final String campo;

    public SolicitudInvalidaException(String campo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
        this.campo = campo;
    }

    @Override
    public Map<String, String> getDetalles() {
        return campo == null ? null : Map.of(campo, getMessage());
    }
}
