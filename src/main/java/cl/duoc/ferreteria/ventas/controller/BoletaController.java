package cl.duoc.ferreteria.ventas.controller;

import cl.duoc.ferreteria.ventas.dto.BoletaRespuesta;
import cl.duoc.ferreteria.ventas.dto.CambioEstadoEntrada;
import cl.duoc.ferreteria.ventas.dto.CompraEntrada;
import cl.duoc.ferreteria.ventas.model.EstadoBoleta;
import cl.duoc.ferreteria.ventas.security.UsuarioAutenticado;
import cl.duoc.ferreteria.ventas.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller sin lógica: valida la entrada, llama al service y devuelve DTOs.
 * Los errores los traduce ManejadorErrores, por eso no hay try/catch.
 */
@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Registro de compras (invitado o cliente), órdenes y estados")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @PostMapping
    @Operation(summary = "Registra una compra",
            description = "Token opcional: sin token compra como invitado (nombre y correo obligatorios); "
                    + "con token CLIENTE usa los datos de la sesión; otros roles → 403")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra registrada; el header Location apunta a la boleta"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: ítems, comprador o RUT"),
            @ApiResponse(responseCode = "401", description = "Token inválido o expirado"),
            @ApiResponse(responseCode = "403", description = "El token es de VENDEDOR o ADMINISTRADOR"),
            @ApiResponse(responseCode = "404", description = "Un producto del carrito no existe en el catálogo"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente"),
            @ApiResponse(responseCode = "503", description = "El catálogo no está disponible")
    })
    public ResponseEntity<BoletaRespuesta> registrarCompra(
            @Valid @RequestBody CompraEntrada entrada,
            @AuthenticationPrincipal @Parameter(hidden = true) UsuarioAutenticado usuario) {
        BoletaRespuesta boleta = boletaService.registrarCompra(entrada, usuario);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(boleta.id()).toUri();
        return ResponseEntity.created(ubicacion).body(boleta);
    }

    @GetMapping
    @Operation(summary = "Lista órdenes, más recientes primero",
            description = "Solo ADMINISTRADOR y VENDEDOR. ?estado= es opcional")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de órdenes"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido"),
            @ApiResponse(responseCode = "403", description = "El rol no puede listar órdenes")
    })
    public List<BoletaRespuesta> listar(@RequestParam(required = false) EstadoBoleta estado) {
        return boletaService.listar(estado);
    }

    @GetMapping("/mias")
    @Operation(summary = "Compras del cliente de la sesión (para \"Mi cuenta\")")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compras del cliente"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido"),
            @ApiResponse(responseCode = "403", description = "Solo CLIENTE")
    })
    public List<BoletaRespuesta> listarMias(
            @AuthenticationPrincipal @Parameter(hidden = true) UsuarioAutenticado usuario) {
        return boletaService.listarMias(usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una orden con sus ítems")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La orden"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido"),
            @ApiResponse(responseCode = "403", description = "ADMINISTRADOR o VENDEDOR"),
            @ApiResponse(responseCode = "404", description = "No existe la boleta")
    })
    public BoletaRespuesta obtener(@PathVariable Long id) {
        return boletaService.obtener(id);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de una orden",
            description = "Solo ADMINISTRADOR. Solo al siguiente estado de la secuencia, "
                    + "o a ANULADA desde uno no final. Anular repone el stock")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La orden con su nuevo estado"),
            @ApiResponse(responseCode = "400", description = "Estado inválido o falta"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido"),
            @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR"),
            @ApiResponse(responseCode = "404", description = "No existe la boleta"),
            @ApiResponse(responseCode = "409", description = "Transición no permitida o catálogo caído")
    })
    public BoletaRespuesta cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody CambioEstadoEntrada entrada) {
        return boletaService.cambiarEstado(id, entrada.estado());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una orden",
            description = "Solo ADMINISTRADOR. Repone el stock si no estaba anulada")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Orden eliminada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido"),
            @ApiResponse(responseCode = "403", description = "Solo ADMINISTRADOR"),
            @ApiResponse(responseCode = "404", description = "No existe la boleta")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        boletaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
