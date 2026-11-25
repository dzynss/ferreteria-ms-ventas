package cl.duoc.ferreteria.ventas.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Cuerpo único de error del contrato (CLAUDE.md §6.1 / C4).
 * `detalles` solo aparece en errores 400 de validación (campo → mensaje).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(String timestamp, int status, String error, String mensaje,
                              String ruta, Map<String, String> detalles) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static ErrorRespuesta de(HttpStatus status, String mensaje, String ruta, Map<String, String> detalles) {
        String ahora = LocalDateTime.now(ZoneId.of("America/Santiago")).format(FORMATO);
        return new ErrorRespuesta(ahora, status.value(), status.getReasonPhrase(), mensaje, ruta, detalles);
    }
}
