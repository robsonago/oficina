package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.LoginRequest;
import br.com.fiap.challange.oficina.dto.request.UsuarioRequest;
import br.com.fiap.challange.oficina.dto.response.LoginResponse;
import br.com.fiap.challange.oficina.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.model.Usuario;
import br.com.fiap.challange.oficina.repository.UsuarioRepository;
import br.com.fiap.challange.oficina.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtService tokenService;
    @Mock
    UserDetailsService userDetailsService;

    @InjectMocks
    AuthService authService;

    @Test
    void deveRealizarLoginComSucesso() {
        LoginRequest request = new LoginRequest("admin", "admin123");
        UserDetails userDetails = new User("admin", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Usuario usuario = usuarioPadrao();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(tokenService.generateToken(userDetails)).thenReturn("jwt-token-test");
        when(tokenService.getExpiration()).thenReturn(86400000L);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token-test");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void deveRegistrarNovoUsuario() {
        UsuarioRequest request = new UsuarioRequest("tecnico1", "senha123", "TECNICO");
        when(usuarioRepository.existsByUsername("tecnico1")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encoded-senha");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> authService.registrar(request)).doesNotThrowAnyException();
        verify(usuarioRepository).save(any());
    }

    @Test
    void deveLancarExcecaoUsuarioDuplicado() {
        UsuarioRequest request = new UsuarioRequest("admin", "senha123", "ADMIN");
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("admin");
    }

    private Usuario usuarioPadrao() {
        return Usuario.builder()
                .id(1L).username("admin").password("encoded")
                .role("ADMIN").ativo(true).createdAt(LocalDateTime.now()).build();
    }
}
