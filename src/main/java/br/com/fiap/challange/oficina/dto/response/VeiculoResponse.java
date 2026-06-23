package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.Veiculo;

import java.time.LocalDateTime;

public record VeiculoResponse(
        Long id,
        String placa,
        String marca,
        String modelo,
        Integer ano,
        Long clienteId,
        String nomeCliente,
        Boolean ativo,
        LocalDateTime createdAt
) {
    public static VeiculoResponse from(Veiculo v) {
        return new VeiculoResponse(
                v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(),
                v.getCliente().getId(), v.getCliente().getNome(), v.getAtivo(), v.getCreatedAt()
        );
    }
}
