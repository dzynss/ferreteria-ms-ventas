package cl.duoc.ferreteria.ventas.client;

import cl.duoc.ferreteria.ventas.dto.ItemDescontado;
import cl.duoc.ferreteria.ventas.dto.ItemStock;
import cl.duoc.ferreteria.ventas.dto.StockDescuentoRespuesta;
import cl.duoc.ferreteria.ventas.dto.StockSolicitud;
import cl.duoc.ferreteria.ventas.exception.ErrorRespuesta;
import cl.duoc.ferreteria.ventas.exception.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Llamadas a la API interna de ms-catalogo. No reenvía el token del usuario:
 * la API interna se autentica con X-API-KEY, no con JWT (ya viene en el RestClient).
 *
 * Traducción de errores (C9.4):
 * - 404 y 409 → se propagan al front con el mismo código y mensaje;
 * - cualquier otro error (ej. 401 por clave mala) → 500, porque un 401 del catálogo
 *   haría creer al front que SU sesión expiró;
 * - sin conexión → 503.
 */
@Component
public class CatalogoClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClient.class);

    private final RestClient restClient;

    public CatalogoClient(RestClient catalogoRestClient) {
        this.restClient = catalogoRestClient;
    }

    public List<ItemDescontado> descontarStock(List<ItemStock> items) {
        try {
            StockDescuentoRespuesta respuesta = restClient.post()
                    .uri("/api/v1/interno/stock/descuento")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockSolicitud(items))
                    .retrieve()
                    .body(StockDescuentoRespuesta.class);
            return respuesta.items();
        } catch (RestClientResponseException e) {
            throw traducir(e);
        } catch (ResourceAccessException e) {   // conexión rechazada o timeout
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El catálogo no está disponible. Intenta nuevamente.");
        }
    }

    public void reponerStock(List<ItemStock> items) {
        try {
            restClient.post()
                    .uri("/api/v1/interno/stock/reposicion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockSolicitud(items))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw traducir(e);
        } catch (ResourceAccessException e) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El catálogo no está disponible. Intenta nuevamente.");
        }
    }

    /** Solo 404 y 409 llegan al front con su código y mensaje originales. */
    private NegocioException traducir(RestClientResponseException e) {
        int codigo = e.getStatusCode().value();
        if (codigo == 404 || codigo == 409) {
            ErrorRespuesta error = e.getResponseBodyAs(ErrorRespuesta.class);
            String mensaje = (error != null && error.mensaje() != null) ? error.mensaje() : "Error del catálogo";
            return new NegocioException(HttpStatus.valueOf(codigo), mensaje);
        }
        log.error("ms-catalogo respondió {}: {}", codigo, e.getResponseBodyAsString());
        return new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al comunicarse con el catálogo");
    }
}
