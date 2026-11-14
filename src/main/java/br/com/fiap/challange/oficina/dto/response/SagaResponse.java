package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.PassoSaga;
import br.com.fiap.challange.oficina.domain.model.SagaOrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;

import java.time.LocalDateTime;
import java.util.List;

public record SagaResponse(String id, Long ordemServicoId, EtapaSaga etapa, String orcamentoId, String pagamentoId,
                           String linkPagamento, boolean estoqueReservado, String motivoCompensacao,
                           LocalDateTime prazo, LocalDateTime criadaEm, LocalDateTime atualizadaEm,
                           List<PassoSaga> passos) {

    public static SagaResponse from(SagaOrdemServico s) {
        return new SagaResponse(s.getId(), s.getOrdemServicoId(), s.getEtapa(), s.getOrcamentoId(), s.getPagamentoId(),
                s.getLinkPagamento(), s.isEstoqueReservado(), s.getMotivoCompensacao(), s.getPrazo(), s.getCriadaEm(),
                s.getAtualizadaEm(), s.getPassos());
    }
}
