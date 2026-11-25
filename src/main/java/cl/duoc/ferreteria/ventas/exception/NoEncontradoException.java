package cl.duoc.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso no existe. */
public class NoEncontradoException extends NegocioException {

    public NoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
