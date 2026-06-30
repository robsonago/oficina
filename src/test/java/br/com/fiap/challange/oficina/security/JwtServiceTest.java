package br.com.fiap.challange.oficina.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.fiap.challange.oficina.infrastructure.security.JwtService;

class JwtServiceTest {

    private static final String SECRET = "bXlTdXBlclNlY3JldEtleUZvckpXVFN5c3RlbU9maWNpbmFNZWNhbmljYTIwMjQ=";
    private static final long EXPIRATION = 86400000L;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    void deveGerarTokenValido() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        assertThat(token).isNotBlank();
    }

    @Test
    void deveExtrairUsernameDoToken() {
        String token = jwtService.generateToken("tecnico", "ROLE_ADMIN");
        assertThat(jwtService.extractUsername(token)).isEqualTo("tecnico");
    }

    @Test
    void deveValidarTokenParaUsuarioCorreto() {
        UserDetails user = userDetails("admin");
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void deveInvalidarTokenParaUsuarioDiferente() {
        UserDetails outro = userDetails("outro");
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");
        assertThat(jwtService.isTokenValid(token, outro)).isFalse();
    }

    @Test
    void deveLancarExcecaoParaTokenExpirado() {
        JwtService serviceExpirado = new JwtService();
        ReflectionTestUtils.setField(serviceExpirado, "secret", SECRET);
        ReflectionTestUtils.setField(serviceExpirado, "expiration", -1000L);

        UserDetails user = userDetails("admin");
        String token = serviceExpirado.generateToken("admin", "ROLE_ADMIN");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> serviceExpirado.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void deveRetornarExpiracaoConfigurada() {
        assertThat(jwtService.getExpiration()).isEqualTo(EXPIRATION);
    }

    @Test
    void deveGerarTokenSemAuthorities() {
        String token = jwtService.generateToken("anon", "");
        assertThat(jwtService.extractUsername(token)).isEqualTo("anon");
    }

    private UserDetails userDetails(String username) {
        return new User(username, "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
