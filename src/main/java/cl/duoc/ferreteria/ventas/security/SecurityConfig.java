package cl.duoc.ferreteria.ventas.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Estructura común del contrato (§7.4) con las reglas propias de ms-ventas (§6.6):
 * - POST /api/v1/boletas público: compra invitado o CLIENTE; el service rechaza otros roles;
 * - /mias solo CLIENTE; GET listado/detalle para ADMINISTRADOR y VENDEDOR;
 * - PATCH de estado y DELETE solo ADMINISTRADOR.
 * Sin CSRF ni sesión: API stateless con JWT en header.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtFiltro jwtFiltro) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/boletas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/mias").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                        .requestMatchers("/api/v1/boletas/**").hasRole("ADMINISTRADOR")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                RespuestaErrorSeguridad.escribir(res, req, 401, "Debes iniciar sesión"))
                        .accessDeniedHandler((req, res, ex) ->
                                RespuestaErrorSeguridad.escribir(res, req, 403, "No tienes permiso para esta acción")))
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JwtFiltro es @Component, así que Spring Boot lo registraría además como filtro de servlet
     * y correría dos veces. Con este registro deshabilitado solo corre dentro de la cadena de
     * Spring Security (obligatorio mantenerlo, contrato §7.4, nota v1.1).
     */
    @Bean
    FilterRegistrationBean<JwtFiltro> registroFiltroJwt(JwtFiltro jwtFiltro) {
        FilterRegistrationBean<JwtFiltro> registro = new FilterRegistrationBean<>(jwtFiltro);
        registro.setEnabled(false);
        return registro;
    }

    /** Evita que Spring Boot cree un usuario en memoria e imprima "Using generated security password". */
    @Bean
    UserDetailsService sinUsuariosEnMemoria() {
        return username -> { throw new UsernameNotFoundException(username); };
    }
}
