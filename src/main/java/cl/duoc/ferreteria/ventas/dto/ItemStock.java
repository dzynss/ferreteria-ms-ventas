package cl.duoc.ferreteria.ventas.dto;

/** Ítem para la API interna de stock de ms-catalogo (mismos nombres JSON que ms-catalogo.md §9.2). */
public record ItemStock(String codigo, Integer cantidad) { }
