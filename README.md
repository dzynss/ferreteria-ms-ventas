# ms-ventas · Ferretería Los Maestros

Microservicio de ventas del proyecto Ferretería Los Maestros (EP3 — Fullstack II, Duoc UC).
Registra compras de invitados y clientes, lista órdenes, cambia estados y anula/elimina boletas.

- **Puerto**: 8083 (`http://localhost:8083`)
- **Swagger**: `http://localhost:8083/swagger-ui.html`
- **Paquete base**: `cl.duoc.ferreteria.ventas`
- **Base de datos**: MySQL 8, schema `db_ventas` (tablas `boleta` y `detalle_boleta`)
- **Stack**: Java 17 · Spring Boot 4.1.3 · Spring Security 7 (JWT) · springdoc 3.2.0 · jjwt 0.12.6

## Endpoints (v1)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/api/v1/boletas` | Registra una compra | Público (invitado o CLIENTE; otros roles → 403) |
| GET | `/api/v1/boletas` | Lista órdenes (`?estado=` opcional) | ADMINISTRADOR, VENDEDOR |
| GET | `/api/v1/boletas/{id}` | Detalle de una orden | ADMINISTRADOR, VENDEDOR |
| GET | `/api/v1/boletas/mias` | Compras del cliente de la sesión | CLIENTE |
| PATCH | `/api/v1/boletas/{id}/estado` | Cambia el estado (o anula) | ADMINISTRADOR |
| DELETE | `/api/v1/boletas/{id}` | Elimina (repone stock si no estaba anulada) | ADMINISTRADOR |

## Cómo funciona

- **Compra**: el front solo envía `comprador` (si es invitado) e `items` (`codigo` + `cantidad`).
  ms-ventas pide a ms-catalogo que descuente el stock por su API interna (`POST /api/v1/interno/stock/descuento`,
  autenticada con el header `X-API-KEY`); **los precios los devuelve ms-catalogo** y el total se calcula en el
  servidor. Si el guardado de la boleta falla después de descontar, se llama a la reposición (**compensación**).
- **Anular / eliminar**: repone el stock de los ítems de la boleta.
- **Estados**: `RECIBIDA → EN_PREPARACION → LISTA_PARA_RETIRO → ENTREGADA`, y `ANULADA` desde cualquiera no final.
  No se permiten saltos, retrocesos ni cambios sobre estados finales (409).
- **Seguridad**: valida el JWT que emite ms-usuarios (mismo `JWT_SECRET`, emisor `ferreteria-ms-usuarios`).
  `POST /boletas` es público; un token de rol VENDEDOR/ADMINISTRADOR recibe 403 desde el service.
  Un token vencido recibe 401 incluso en endpoints públicos, para que el front renueve la sesión.

## Levantar en local

1. Tener corriendo MySQL (por ejemplo `docker compose up -d mysql` desde `ferreteria-los-maestros/despliegue/`).
2. Crear `.env` a partir de `.env.example` y completar los valores.
3. Ejecutar `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
4. Abrir `http://localhost:8083/swagger-ui.html`.

Para probar el flujo completo de compra, ms-catalogo debe estar corriendo en `CATALOGO_URL`
(`docker compose up -d mysql ms-catalogo`).

## Variables de entorno (`.env`)

| Variable | Ejemplo | Nota |
|---|---|---|
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | `mysql` dentro de Docker |
| `DB_USER` / `DB_PASSWORD` | `ferreteria` / … | Usuario de aplicación de MySQL |
| `JWT_SECRET` | Base64 de 32+ bytes | **El mismo valor en los 3 servicios** |
| `INTERNAL_API_KEY` | … | **El mismo valor que ms-catalogo** |
| `CATALOGO_URL` | `http://localhost:8082` | `http://ms-catalogo:8082` en Docker |

## Tests

```bash
./mvnw test
```

Incluye tests unitarios del flujo de compra y la compensación (`BoletaServiceTest`), de la traducción de
errores del catálogo (`CatalogoClientTest`), de las transiciones de estado (`EstadoBoletaTest`), del
validador de RUT (`ValidadorRutTest`) y de las reglas 401/403 (`SeguridadVentasTest` con tokens de prueba).

## Estructura

```
cl.duoc.ferreteria.ventas
├── controller/  BoletaController (REST, sin lógica)
├── service/     BoletaService (reglas de negocio y compensación de stock)
├── model/       Boleta, DetalleBoleta, EstadoBoleta
├── repository/  BoletaRepository
├── dto/         records de entrada (validación) y salida
├── security/    JwtUtil, JwtFiltro, SecurityConfig, UsuarioAutenticado, RespuestaErrorSeguridad
├── exception/  NegocioException + subclases, ErrorRespuesta, ManejadorErrores
├── client/      CatalogoClient (RestClient → ms-catalogo)
├── config/      OpenApiConfig, RestClientConfig
└── util/        ValidadorRut (módulo 11)
```
