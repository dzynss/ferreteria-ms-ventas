package cl.duoc.ferreteria.ventas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Datos del comprador invitado. La obligatoriedad de nombre y correo NO va aquí:
 * la decide el service según si la compra viene de invitado o de un CLIENTE con sesión.
 */
public record CompradorEntrada(
        @Size(max = 120) String nombre,
        @Email(message = "Formato de correo inválido") @Size(max = 120) String correo,
        String rut) { }
