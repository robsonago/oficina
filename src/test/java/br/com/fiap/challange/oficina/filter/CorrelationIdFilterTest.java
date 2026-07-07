package br.com.fiap.challange.oficina.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.fiap.challange.oficina.infrastructure.filter.CorrelationIdFilter;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveUsarCorrelationIdDoHeaderQuandoPresente() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("meu-id-externo");

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-Correlation-Id", "meu-id-externo");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveGerarCorrelationIdParaUsuarioAnonimo() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveGerarCorrelationIdParaUsuarioAutenticado() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        User userDetails = new User("admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deveGerarCorrelationIdQuandoHeaderEstaBranco() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("   ");

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(eq("X-Correlation-Id"), anyString());
        verify(filterChain).doFilter(request, response);
    }
}
