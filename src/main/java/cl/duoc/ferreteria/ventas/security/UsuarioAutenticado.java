package cl.duoc.ferreteria.ventas.security;

/**
 * Usuario extraído del access token. Llega como principal a los controllers
 * (null si la petición no trae token).
 */
public record UsuarioAutenticado(Long id, String correo, String nombre, String rol) { }
