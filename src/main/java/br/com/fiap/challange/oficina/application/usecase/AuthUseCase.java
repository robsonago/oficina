package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.AuthToken;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.in.AuthInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.LoginCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.RegistrarUsuarioCommand;
import br.com.fiap.challange.oficina.domain.port.out.AuthenticationPort;
import br.com.fiap.challange.oficina.domain.port.out.PasswordEncoderPort;
import br.com.fiap.challange.oficina.domain.port.out.TokenPort;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthUseCase implements AuthInputPort {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final AuthenticationPort authenticationPort;
    private final TokenPort tokenPort;

    @Override
    public AuthToken login(LoginCommand command) {
        log.info("Tentativa de login username={}", command.username());
        authenticationPort.autenticar(command.username(), command.password());
        Usuario usuario = usuarioRepository.findByUsername(command.username()).orElseThrow();
        String token = tokenPort.generateToken(usuario.getUsername(), usuario.getRole());
        log.info("Login bem-sucedido username={} role={}", usuario.getUsername(), usuario.getRole());
        return new AuthToken(token, "Bearer", usuario.getUsername(), usuario.getRole(), tokenPort.getExpiration());
    }

    @Override
    public void registrar(RegistrarUsuarioCommand command) {
        log.info("Registrando usuário username={} role={}", command.username(), command.role());
        if (usuarioRepository.existsByUsername(command.username())) {
            throw new RegraDeNegocioException("Usuário já existe: " + command.username());
        }
        Usuario usuario = Usuario.builder()
                .username(command.username())
                .password(passwordEncoder.encode(command.password()))
                .role(command.role())
                .build();
        usuarioRepository.save(usuario);
        log.info("Usuário registrado username={}", command.username());
    }
}
