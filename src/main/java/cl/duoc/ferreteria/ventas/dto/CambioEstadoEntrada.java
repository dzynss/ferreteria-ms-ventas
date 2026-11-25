package cl.duoc.ferreteria.ventas.dto;

import cl.duoc.ferreteria.ventas.model.EstadoBoleta;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoEntrada(
        @NotNull(message = "El estado es obligatorio") EstadoBoleta estado) { }
