package br.com.fiap.challange.oficina.controller;

import br.com.fiap.challange.oficina.dto.request.LoginRequest;
import br.com.fiap.challange.oficina.dto.request.UsuarioRequest;
import br.com.fiap.challange.oficina.dto.response.LoginResponse;
import br.com.fiap.challange.oficina.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e gestão de usuários")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Realizar login e obter token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar novo usuário (somente ADMIN)")
    public ResponseEntity<Void> registrar(@Valid @RequestBody UsuarioRequest request) {
        authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
