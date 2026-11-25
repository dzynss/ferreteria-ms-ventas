package cl.duoc.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

/** 403: token válido pero el rol no puede hacer esta acción. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
