package com.raissa.rpa.config;

import com.raissa.rpa.service.AuthService;
import com.raissa.rpa.service.ValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final ValidationService validationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");
        final String transactionId = request.getHeader("X-Transaction-ID");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                // Validar token usando AuthService
                var claims = authService.decodeToken(jwtToken);
                username = (String) claims.get("sub");

                log.debug("Token validado para usuario: {}, transactionId: {}", username, transactionId);

            } catch (Exception e) {
                logger.error("Token JWT inválido: {}" + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\": false, \"message\": \"Token inválido o expirado\"}");
                return;
            }
        } else {
            logger.warn("Header Authorization no encontrado o formato incorrecto");
        }

        // Validar token y transactionId si están presentes
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                // Validar también la sesión con transactionId si está presente
                if (transactionId != null && !transactionId.isEmpty()) {
                    validationService.validateSession(transactionId);
                    log.debug("Sesión validada para transactionId: {}", transactionId);
                }

                // Crear autenticación Spring Security
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("Usuario autenticado: {}, transactionId: {}", username, transactionId);

            } catch (Exception e) {
                logger.error("Error validando sesión: {}" + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/validate") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }
}
