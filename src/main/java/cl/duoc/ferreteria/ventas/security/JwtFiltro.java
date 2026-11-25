package cl.duoc.ferreteria.ventas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reglas del filtro JWT (contrato §6.2):
 * - sin header Authorization → sigue anónimo (los endpoints públicos funcionan);
 * - con token válido de acceso → autentica con autoridad ROLE_<rol>;
 * - con token inválido o expirado → 401 aunque el endpoint sea público,
 *   para que el front renueve la sesión en vez de comprar como invitado por error.
 */
@Component
public class JwtFiltro extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFiltro(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);   // sin token: sigue como anónimo
            return;
        }

        try {
            Claims claims = jwtUtil.validarAccessToken(header.substring(7));
            String rol = claims.get("rol", String.class);
            UsuarioAutenticado usuario = new UsuarioAutenticado(
                    Long.valueOf(claims.getSubject()),
                    claims.get("correo", String.class),
                    claims.get("nombre", String.class),
                    rol);
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            RespuestaErrorSeguridad.escribir(response, request, 401, "Token inválido o expirado");
            return;
        }

        chain.doFilter(request, response);
    }
}
