package cl.duoc.ferreteria.ventas.dto;

import cl.duoc.ferreteria.ventas.model.Boleta;
import cl.duoc.ferreteria.ventas.model.EstadoBoleta;

import java.time.LocalDateTime;
import java.util.List;

/** `items` (no `detalles`): es el nombre del contrato (§6.6) y el que usa el front. */
public record BoletaRespuesta(Long id, LocalDateTime fecha, Long usuarioId, String nombreCliente,
                              String correoCliente, String rutCliente, EstadoBoleta estado, Long total,
                              List<DetalleBoletaRespuesta> items) {

    public static BoletaRespuesta desde(Boleta b) {
        return new BoletaRespuesta(b.getId(), b.getFecha(), b.getUsuarioId(), b.getNombreCliente(),
                b.getCorreoCliente(), b.getRutCliente(), b.getEstado(), b.getTotal(),
                b.getDetalles().stream()
                        .map(d -> new DetalleBoletaRespuesta(d.getProductoCodigo(), d.getNombreProducto(),
                                d.getPrecioUnitario(), d.getCantidad(), d.getSubtotal()))
                        .toList());
    }
}
