package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record OrdemServicoRequest(
        @NotBlank(message = "Documento do cliente é obrigatório")
        String documentoCliente,

        @NotBlank(message = "Placa do veículo é obrigatória")
        String placaVeiculo,

        String descricaoProblema,
        String observacoes,

        @Valid
        List<ItemServicoRequest> servicos,

        @Valid
        List<ItemPecaRequest> pecas
) {
}
