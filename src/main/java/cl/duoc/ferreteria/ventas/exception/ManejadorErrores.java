package cl.duoc.ferreteria.ventas.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global: traduce toda excepción al JSON de C4.
 * Así los controllers no tienen try/catch y todos los errores se ven iguales para el front.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ErrorRespuesta> negocio(NegocioException ex, HttpServletRequest req) {
        return responder(ex.getStatus(), ex.getMessage(), req, ex.getDetalles());
    }

    /** Bean Validation en @RequestBody: cada campo con su mensaje, sin repetir campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> detalles.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", req, detalles);
    }

    /** JSON roto, enum o número inválido: sin exponer detalles internos. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorRespuesta> malFormada(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "La solicitud está mal formada", req, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> otro(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse er) {   // errores propios de Spring MVC: 405, 415, ruta inexistente…
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return responder(status, status.getReasonPhrase(), req, null);
        }
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus status, String mensaje,
                                                    HttpServletRequest req, Map<String, String> detalles) {
        return ResponseEntity.status(status).body(ErrorRespuesta.de(status, mensaje, req.getRequestURI(), detalles));
    }
}
