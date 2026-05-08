package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ServicoRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        String descricao,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal preco,

        @Min(value = 1, message = "Tempo estimado deve ser pelo menos 1 minuto")
        Integer tempoEstimadoMinutos
) {
}
