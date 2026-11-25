package cl.duoc.ferreteria.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP hacia ms-catalogo: baseUrl configurable (localhost en desarrollo,
 * nombre de Docker en producción) y la clave interna X-API-KEY en cada llamada.
 * Timeouts cortos: si el catálogo no responde, la compra falla rápido con 503.
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient catalogoRestClient(@Value("${catalogo.url}") String url,
                                  @Value("${interno.api-key}") String claveInterna) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("X-API-KEY", claveInterna)
                .requestFactory(fabrica)
                .build();
    }
}
