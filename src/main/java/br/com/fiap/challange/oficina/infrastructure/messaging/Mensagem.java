package br.com.fiap.challange.oficina.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope padrão trocado entre os serviços da oficina pelo Pub/Sub.
 * O mesmo formato é usado pelo OS Service e pelo Execution Service, então
 * qualquer mudança aqui precisa ser replicada nos outros repositórios.
 *
 * @param id             identificador único da mensagem (usado para idempotência)
 * @param tipo           comando ou evento (ex.: GERAR_ORCAMENTO, ORCAMENTO_GERADO)
 * @param sagaId         saga a que a mensagem pertence
 * @param ordemServicoId OS de origem
 * @param ocorridoEm     momento em que a mensagem foi criada
 * @param dados          conteúdo específico do tipo
 */
public record Mensagem(String id, String tipo, String sagaId, Long ordemServicoId,
                       Instant ocorridoEm, JsonNode dados) {

    public static Mensagem nova(String tipo, String sagaId, Long ordemServicoId, JsonNode dados) {
        return new Mensagem(UUID.randomUUID().toString(), tipo, sagaId, ordemServicoId, Instant.now(), dados);
    }
}
