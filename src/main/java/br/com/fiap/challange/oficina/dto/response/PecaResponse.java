package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.model.Peca;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PecaResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal precoUnitario,
        Integer quantidadeEstoque,
        String codigoReferencia,
        Boolean ativo,
        LocalDateTime createdAt
) {
    public static PecaResponse from(Peca p) {
        return new PecaResponse(
                p.getId(), p.getNome(), p.getDescricao(), p.getPrecoUnitario(),
                p.getQuantidadeEstoque(), p.getCodigoReferencia(), p.getAtivo(), p.getCreatedAt()
        );
    }
}
