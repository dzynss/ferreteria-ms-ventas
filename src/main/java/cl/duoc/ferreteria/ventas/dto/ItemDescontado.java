package cl.duoc.ferreteria.ventas.dto;

/**
 * Lo que devuelve ms-catalogo al descontar stock.
 * precioUnitario es la ÚNICA fuente de precios para la boleta.
 */
public record ItemDescontado(String codigo, String nombre, Integer precioUnitario, Integer cantidad) { }
