package cl.duoc.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

/** 409: conflicto de negocio (stock insuficiente, cambio de estado inválido). */
public class ConflictoException extends NegocioException {

    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
