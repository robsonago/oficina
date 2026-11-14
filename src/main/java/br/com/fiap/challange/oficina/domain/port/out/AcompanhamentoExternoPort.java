package br.com.fiap.challange.oficina.domain.port.out;

import java.util.Optional;

/**
 * Consulta síncrona (REST) aos outros serviços, usada só para montar a visão
 * consolidada da OS. Devolve vazio quando o serviço não responde — a tela do
 * cliente não pode cair porque o Billing está fora.
 */
public interface AcompanhamentoExternoPort {

    Optional<Object> orcamentos(Long ordemServicoId);

    Optional<Object> execucoes(Long ordemServicoId);
}
