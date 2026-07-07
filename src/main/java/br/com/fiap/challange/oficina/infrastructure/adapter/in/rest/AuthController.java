package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.port.in.AuthInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.LoginCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.RegistrarUsuarioCommand;
import br.com.fiap.challange.oficina.dto.request.LoginRequest;
import br.com.fiap.challange.oficina.dto.request.UsuarioRequest;
import br.com.fiap.challange.oficina.dto.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e gestão de usuários")
public class AuthController {

    private final AuthInputPort authUseCase;

    @PostMapping("/login")
    @Operation(summary = "Realizar login e obter token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.username(), request.password());
        return ResponseEntity.ok(LoginResponse.from(authUseCase.login(command)));
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar novo usuário (somente ADMIN)")
    public ResponseEntity<Void> registrar(@Valid @RequestBody UsuarioRequest request) {
        RegistrarUsuarioCommand command = new RegistrarUsuarioCommand(request.username(), request.password(), request.role());
        authUseCase.registrar(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
