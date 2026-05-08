package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PecaRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        String descricao,

        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal precoUnitario,

        @Min(value = 0, message = "Quantidade em estoque não pode ser negativa")
        Integer quantidadeEstoque,

        @Size(max = 100)
        String codigoReferencia
) {
}
