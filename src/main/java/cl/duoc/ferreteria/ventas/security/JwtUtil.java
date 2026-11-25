package cl.duoc.ferreteria.ventas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Valida (no genera) los tokens que emite ms-usuarios: misma clave HS256 y emisor exigido.
 * La generación vive solo en ms-usuarios.
 */
@Component
public class JwtUtil {

    public static final String EMISOR = "ferreteria-ms-usuarios";

    private final SecretKey clave;

    public JwtUtil(@Value("${jwt.secret}") String secretoBase64) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
    }

    /** Devuelve los claims si el token es válido, de tipo "access" y no expiró. Si no, lanza JwtException. */
    public Claims validarAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("tipo", String.class))) {
            throw new JwtException("El token no es de acceso");
        }
        return claims;
    }
}
