package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPecaRequest(
        @NotNull(message = "ID da peça é obrigatório")
        Long pecaId,

        @Min(value = 1, message = "Quantidade mínima é 1")
        @NotNull(message = "Quantidade é obrigatória")
        Integer quantidade
) {
}
