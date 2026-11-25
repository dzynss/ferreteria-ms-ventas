package cl.duoc.ferreteria.ventas.security;

import cl.duoc.ferreteria.ventas.client.CatalogoClient;
import cl.duoc.ferreteria.ventas.dto.ItemDescontado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de las reglas de seguridad de la sección 7 de la especificación:
 * cada fila de la tabla, con tokens reales generados por TokenDePrueba (C6)
 * y el catálogo simulado para no salir por HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadVentasTest {

    private static final String COMPRA_INVITADO = """
            {"comprador":{"nombre":"Juan Pérez","correo":"juan@correo.cl","rut":"12345678-5"},
             "items":[{"codigo":"MC001","cantidad":2}]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogoClient catalogoClient;

    @BeforeEach
    void catalogoRespondeBien() {
        when(catalogoClient.descontarStock(any())).thenReturn(List.of(
                new ItemDescontado("MC001", "Cemento Polpaico gris 25 kg", 5990, 2)));
    }

    @Test
    void invitadoSinTokenPuedeComprar() throws Exception {
        mockMvc.perform(post("/api/v1/boletas")
                        .contentType(MediaType.APPLICATION_JSON).content(COMPRA_INVITADO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.usuarioId").value(nullValue()))   // compra de invitado
                .andExpect(jsonPath("$.nombreCliente").value("Juan Pérez"))
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.total").value(11980));
    }

    @Test
    void clienteConTokenCompraConLosDatosDeLaSesion() throws Exception {
        mockMvc.perform(post("/api/v1/boletas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access(12, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"codigo\":\"MC001\",\"cantidad\":2}]}"))   // comprador ignorado
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(12))
                .andExpect(jsonPath("$.nombreCliente").value("Usuario Prueba"))
                .andExpect(jsonPath("$.correoCliente").value("prueba@ferreteria.cl"))
                .andExpect(jsonPath("$.rutCliente").value(nullValue()));   // C9.10
    }

    @Test
    void vendedorYAdministradorNoPuedenComprar() throws Exception {
        for (String rol : new String[]{"VENDEDOR", "ADMINISTRADOR"}) {
            mockMvc.perform(post("/api/v1/boletas")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access(1, rol))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\":[{\"codigo\":\"MC001\",\"cantidad\":2}]}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    void tokenVencidoDa401AunqueElEndpointSeaPublico() throws Exception {
        mockMvc.perform(post("/api/v1/boletas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.expirado("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(COMPRA_INVITADO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"));
    }

    @Test
    void unRefreshTokenNoSirveComoAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/boletas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.refresh()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinToken_ListarBoletasDa401() throws Exception {
        mockMvc.perform(get("/api/v1/boletas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión"));
    }

    @Test
    void vendedorPuedeListarBoletas() throws Exception {
        mockMvc.perform(get("/api/v1/boletas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access(2, "VENDEDOR")))
                .andExpect(status().isOk());
    }

    @Test
    void vendedorNoPuedeCambiarEstado() throws Exception {
        mockMvc.perform(patch("/api/v1/boletas/1/estado")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access(2, "VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permiso para esta acción"));
    }

    @Test
    void vendedorNoPuedeVerBoletasMias() throws Exception {
        mockMvc.perform(get("/api/v1/boletas/mias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access(2, "VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientePuedeVerSusBoletasMias() throws Exception {
        mockMvc.perform(get("/api/v1/boletas/mias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access(12, "CLIENTE")))
                .andExpect(status().isOk());
    }
}
