package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.NotNull;

public record AprovacaoOrcamentoRequest(
        @NotNull(message = "Campo 'aprovado' é obrigatório")
        Boolean aprovado,
        String observacao
) {
}
