package cl.duoc.ferreteria.ventas.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Escribe el JSON de error (C4) desde el filtro JWT y los manejadores 401/403,
 * que corren antes de los controllers. Los mensajes son textos fijos sin comillas,
 * por eso se puede armar el JSON a mano sin escapar.
 */
public final class RespuestaErrorSeguridad {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private RespuestaErrorSeguridad() { }

    public static void escribir(HttpServletResponse response, HttpServletRequest request,
                                int status, String mensaje) throws IOException {
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","mensaje":"%s","ruta":"%s"}"""
                .formatted(LocalDateTime.now(ZoneId.of("America/Santiago")).format(FORMATO),
                        status, HttpStatus.valueOf(status).getReasonPhrase(),
                        mensaje, request.getRequestURI());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }
}
