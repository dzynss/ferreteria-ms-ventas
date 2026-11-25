package cl.duoc.ferreteria.ventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * El front solo envía códigos y cantidades: NUNCA precios ni totales.
 * Los precios los devuelve ms-catalogo al descontar el stock y el total se calcula en el servidor.
 */
public record CompraEntrada(
        @Valid CompradorEntrada comprador,
        @NotEmpty(message = "El carrito está vacío") @Valid List<ItemCompraEntrada> items) { }
