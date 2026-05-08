package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "Username é obrigatório")
        @Size(min = 3, max = 100)
        String username,

        @NotBlank(message = "Password é obrigatório")
        @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
        String password,

        @NotBlank(message = "Role é obrigatória")
        @Pattern(regexp = "ADMIN|TECNICO", message = "Role deve ser ADMIN ou TECNICO")
        String role
) {
}
