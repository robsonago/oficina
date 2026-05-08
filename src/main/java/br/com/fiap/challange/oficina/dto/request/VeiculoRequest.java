package br.com.fiap.challange.oficina.dto.request;

import jakarta.validation.constraints.*;

public record VeiculoRequest(
        @NotBlank(message = "Placa é obrigatória")
        String placa,

        @NotBlank(message = "Marca é obrigatória")
        @Size(max = 100)
        String marca,

        @NotBlank(message = "Modelo é obrigatório")
        @Size(max = 100)
        String modelo,

        @NotNull(message = "Ano é obrigatório")
        @Min(value = 1900, message = "Ano inválido")
        @Max(value = 2100, message = "Ano inválido")
        Integer ano,

        @NotNull(message = "ID do cliente é obrigatório")
        Long clienteId
) {
}
