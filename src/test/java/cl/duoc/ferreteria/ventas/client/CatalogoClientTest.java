package cl.duoc.ferreteria.ventas.client;

import cl.duoc.ferreteria.ventas.dto.ItemDescontado;
import cl.duoc.ferreteria.ventas.dto.ItemStock;
import cl.duoc.ferreteria.ventas.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * MockRestServiceServer intercepta las llamadas HTTP del RestClient sin red real:
 * aquí se prueban las traducciones de errores del catálogo (C9.4).
 */
class CatalogoClientTest {

    private static final String URL_DESCUENTO = "http://catalogo/api/v1/interno/stock/descuento";
    private static final String URL_REPOSICION = "http://catalogo/api/v1/interno/stock/reposicion";

    private MockRestServiceServer servidor;
    private CatalogoClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://catalogo")
                .defaultHeader("X-API-KEY", "clave-interna-de-prueba");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new CatalogoClient(builder.build());
    }

    @Test
    void descuentoExitoso_DevuelveLosItemsConPreciosYEnviaLaClaveInterna() {
        servidor.expect(requestTo(URL_DESCUENTO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "clave-interna-de-prueba"))
                .andRespond(withSuccess(
                        "{\"items\":[{\"codigo\":\"MC001\",\"nombre\":\"Cemento Polpaico gris 25 kg\",\"precioUnitario\":5990,\"cantidad\":2}]}",
                        MediaType.APPLICATION_JSON));

        List<ItemDescontado> items = cliente.descontarStock(List.of(new ItemStock("MC001", 2)));

        assertEquals(1, items.size());
        assertEquals("MC001", items.get(0).codigo());
        assertEquals(5990, items.get(0).precioUnitario());
        servidor.verify();
    }

    @Test
    void error409DelCatalogo_SeTraduceConElMismoCodigoYMensaje() {
        servidor.expect(requestTo(URL_DESCUENTO))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"timestamp\":\"2026-11-23T10:00:00\",\"status\":409,\"error\":\"Conflict\",\"mensaje\":\"Stock insuficiente para HE004: solicitado 5, disponible 4\",\"ruta\":\"/api/v1/interno/stock/descuento\"}"));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> cliente.descontarStock(List.of(new ItemStock("HE004", 5))));

        assertEquals(409, ex.getStatus().value());
        assertEquals("Stock insuficiente para HE004: solicitado 5, disponible 4", ex.getMessage());
    }

    @Test
    void error404DelCatalogo_SeTraduceConElMismoCodigoYMensaje() {
        servidor.expect(requestTo(URL_DESCUENTO))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"timestamp\":\"2026-11-23T10:00:00\",\"status\":404,\"error\":\"Not Found\",\"mensaje\":\"No existe el producto ZZ999\",\"ruta\":\"/api/v1/interno/stock/descuento\"}"));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> cliente.descontarStock(List.of(new ItemStock("ZZ999", 1))));

        assertEquals(404, ex.getStatus().value());
        assertEquals("No existe el producto ZZ999", ex.getMessage());
    }

    @Test
    void error401DelCatalogo_NuncaSeReenviaAlFront() {
        servidor.expect(requestTo(URL_DESCUENTO))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> cliente.descontarStock(List.of(new ItemStock("MC001", 1))));

        assertEquals(500, ex.getStatus().value());   // el front no debe creer que SU sesión expiró
    }

    @Test
    void reposicionExitosa_RespondeSinExcepcion() {
        servidor.expect(requestTo(URL_REPOSICION))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> cliente.reponerStock(List.of(new ItemStock("MC001", 2))));
        servidor.verify();
    }

    @Test
    void catalogoCaido_Da503() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        CatalogoClient caido = new CatalogoClient(RestClient.builder()
                .baseUrl("http://localhost:1")   // nada escucha aquí: conexión rechazada
                .requestFactory(fabrica)
                .build());

        NegocioException ex = assertThrows(NegocioException.class,
                () -> caido.descontarStock(List.of(new ItemStock("MC001", 1))));

        assertEquals(503, ex.getStatus().value());
        assertEquals("El catálogo no está disponible. Intenta nuevamente.", ex.getMessage());
    }
}
