package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.in.AuthInputPort;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import br.com.fiap.challange.oficina.dto.request.LoginRequest;
import br.com.fiap.challange.oficina.dto.request.UsuarioRequest;
import br.com.fiap.challange.oficina.dto.response.LoginResponse;
import br.com.fiap.challange.oficina.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthUseCase implements AuthInputPort {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService tokenService;
    private final UserDetailsService userDetailsService;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Tentativa de login username={}", request.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = tokenService.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByUsername(request.username()).orElseThrow();
        log.info("Login bem-sucedido username={} role={}", usuario.getUsername(), usuario.getRole());
        return new LoginResponse(token, "Bearer", usuario.getUsername(), usuario.getRole(), tokenService.getExpiration());
    }

    @Override
    public void registrar(UsuarioRequest request) {
        log.info("Registrando usuário username={} role={}", request.username(), request.role());
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new RegraDeNegocioException("Usuário já existe: " + request.username());
        }
        Usuario usuario = Usuario.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        usuarioRepository.save(usuario);
        log.info("Usuário registrado username={}", request.username());
    }
}
