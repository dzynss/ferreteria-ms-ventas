package cl.duoc.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

/** 401: sin token o credenciales incorrectas. */
public class NoAutorizadoException extends NegocioException {

    public NoAutorizadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
