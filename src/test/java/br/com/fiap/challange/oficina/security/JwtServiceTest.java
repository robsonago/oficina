package br.com.fiap.challange.oficina.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        UserDetails user = userDetails("admin");
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void deveExtrairUsernameDoToken() {
        UserDetails user = userDetails("tecnico");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("tecnico");
    }

    @Test
    void deveValidarTokenParaUsuarioCorreto() {
        UserDetails user = userDetails("admin");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void deveInvalidarTokenParaUsuarioDiferente() {
        UserDetails admin = userDetails("admin");
        UserDetails outro = userDetails("outro");
        String token = jwtService.generateToken(admin);
        assertThat(jwtService.isTokenValid(token, outro)).isFalse();
    }

    @Test
    void deveLancarExcecaoParaTokenExpirado() {
        JwtService serviceExpirado = new JwtService();
        ReflectionTestUtils.setField(serviceExpirado, "secret", SECRET);
        ReflectionTestUtils.setField(serviceExpirado, "expiration", -1000L);

        UserDetails user = userDetails("admin");
        String token = serviceExpirado.generateToken(user);
        // JJWT lança ExpiredJwtException ao parsear — o filtro captura essa exceção
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> serviceExpirado.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void deveRetornarExpiracaoConfigurada() {
        assertThat(jwtService.getExpiration()).isEqualTo(EXPIRATION);
    }

    @Test
    void deveGerarTokenSemAuthorities() {
        UserDetails user = new User("anon", "pass", List.of());
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("anon");
    }

    private UserDetails userDetails(String username) {
        return new User(username, "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
