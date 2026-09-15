package br.com.fiap.challange.oficina.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_CLIENTE = "CLIENTE";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String subject = jwtService.extractUsername(jwt);
            final String role = jwtService.extractRole(jwt);
            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = ROLE_CLIENTE.equals(role)
                        ? clienteUserDetails(subject)
                        : funcionarioUserDetails(jwt, subject);
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Token emitido pela function de autenticação por CPF: o assunto é o
     * documento do cliente, não um usuário da tabela de funcionários — não
     * passa pelo UserDetailsService. A assinatura/expiração já foram
     * validadas na extração das claims.
     */
    private UserDetails clienteUserDetails(String documento) {
        return new User(documento, "", List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_CLIENTE)));
    }

    private UserDetails funcionarioUserDetails(String jwt, String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return jwtService.isTokenValid(jwt, userDetails) ? userDetails : null;
    }
}
