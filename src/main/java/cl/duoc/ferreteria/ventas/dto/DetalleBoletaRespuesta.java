package cl.duoc.ferreteria.ventas.dto;

public record DetalleBoletaRespuesta(String codigo, String nombre, Integer precioUnitario,
                                     Integer cantidad, Long subtotal) { }
