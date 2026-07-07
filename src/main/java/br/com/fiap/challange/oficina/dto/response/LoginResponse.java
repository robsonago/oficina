package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.AuthToken;

public record LoginResponse(
        String token,
        String tipo,
        String username,
        String role,
        Long expiresIn
) {
    public static LoginResponse from(AuthToken token) {
        return new LoginResponse(token.token(), token.tipo(), token.username(), token.role(), token.expiresIn());
    }
}
