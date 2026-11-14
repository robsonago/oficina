package br.com.fiap.challange.oficina.domain.model;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;

import java.time.LocalDateTime;

/**
 * Uma linha do histórico da OS.
 *
 * @param origem quem provocou a mudança: um usuário, "saga", "billing-service", "execution-service"...
 */
public record HistoricoStatusOS(Long id, Long ordemServicoId, StatusOS statusAnterior, StatusOS statusNovo,
                                String origem, String observacao, LocalDateTime ocorridoEm) {
}
