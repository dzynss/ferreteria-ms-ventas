package cl.duoc.ferreteria.ventas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemCompraEntrada(
        @NotBlank(message = "El código es obligatorio") String codigo,
        @NotNull
        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = 999, message = "La cantidad máxima es 999")
        Integer cantidad) { }
