package br.com.fiap.challange.oficina.dto.response;

public record LoginResponse(
        String token,
        String tipo,
        String username,
        String role,
        Long expiresIn
) {
}
