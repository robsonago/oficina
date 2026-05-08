package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "Documento (CPF/CNPJ) é obrigatório")
        String documento,

        @Email(message = "Email inválido")
        String email,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone,

        @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
        String endereco
) {
}
