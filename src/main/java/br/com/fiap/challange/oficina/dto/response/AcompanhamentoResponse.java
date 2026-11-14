package br.com.fiap.challange.oficina.dto.response;

import java.util.List;

/**
 * Visão consolidada da OS, montada pelo OS Service consultando os outros
 * serviços via REST. Quando um deles não responde, o campo correspondente
 * vem nulo e aparece em {@code indisponiveis}.
 */
public record AcompanhamentoResponse(OrdemServicoResponse ordemServico,
                                     List<HistoricoStatusResponse> historico,
                                     List<SagaResponse> sagas,
                                     Object orcamentos,
                                     Object execucoes,
                                     List<String> indisponiveis) {
}
