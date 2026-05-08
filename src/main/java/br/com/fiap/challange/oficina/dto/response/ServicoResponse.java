package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.model.Servico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer tempoEstimadoMinutos,
        Boolean ativo,
        LocalDateTime createdAt
) {
    public static ServicoResponse from(Servico s) {
        return new ServicoResponse(
                s.getId(), s.getNome(), s.getDescricao(), s.getPreco(),
                s.getTempoEstimadoMinutos(), s.getAtivo(), s.getCreatedAt()
        );
    }
}
