package cl.duoc.ferreteria.ventas.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger con el botón "Authorize": cada endpoint protegido declara
 * @SecurityRequirement(name = "bearerAuth") y así se puede probar con el token.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "ms-ventas · Ferretería Los Maestros", version = "v1",
        description = "Registro de compras (invitado o cliente), listado de órdenes, cambio de estado y anulación"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig { }
