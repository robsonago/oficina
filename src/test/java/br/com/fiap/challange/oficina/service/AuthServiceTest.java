package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.AuthToken;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.in.command.LoginCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.RegistrarUsuarioCommand;
import br.com.fiap.challange.oficina.domain.port.out.AuthenticationPort;
import br.com.fiap.challange.oficina.domain.port.out.PasswordEncoderPort;
import br.com.fiap.challange.oficina.domain.port.out.TokenPort;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.fiap.challange.oficina.application.usecase.AuthUseCase;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UsuarioRepositoryPort usuarioRepository;
    @Mock
    PasswordEncoderPort passwordEncoder;
    @Mock
    AuthenticationPort authenticationPort;
    @Mock
    TokenPort tokenPort;

    @InjectMocks
    AuthUseCase authService;

    @Test
    void deveRealizarLoginComSucesso() {
        LoginCommand command = new LoginCommand("admin", "admin123");
        Usuario usuario = usuarioPadrao();

        when(tokenPort.generateToken("admin", "ADMIN")).thenReturn("jwt-token-test");
        when(tokenPort.getExpiration()).thenReturn(86400000L);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        AuthToken response = authService.login(command);

        assertThat(response.token()).isEqualTo("jwt-token-test");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void deveRegistrarNovoUsuario() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand("tecnico1", "senha123", "TECNICO");
        when(usuarioRepository.existsByUsername("tecnico1")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encoded-senha");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> authService.registrar(command)).doesNotThrowAnyException();
        verify(usuarioRepository).save(any());
    }

    @Test
    void deveLancarExcecaoUsuarioDuplicado() {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand("admin", "senha123", "ADMIN");
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(command))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("admin");
    }

    private Usuario usuarioPadrao() {
        return Usuario.builder()
                .id(1L).username("admin").password("encoded")
                .role("ADMIN").ativo(true).createdAt(LocalDateTime.now()).build();
    }
}
