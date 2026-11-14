package br.com.fiap.challange.oficina.infrastructure.adapter.out.mensageria;

import br.com.fiap.challange.oficina.domain.port.out.ComandosSagaPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.OutboxJpaEntity;
import br.com.fiap.challange.oficina.infrastructure.messaging.Mensagem;
import br.com.fiap.challange.oficina.infrastructure.messaging.Rastreamento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Os comandos da saga vão para a outbox, na mesma transação que muda a OS e
 * a saga. Assim nunca acontece de a OS ir para "aguardando orçamento" sem o
 * pedido de orçamento sair (ou o contrário). O {@link RelayOutbox} publica.
 */
@Component
@RequiredArgsConstructor
public class OutboxComandosAdapter implements ComandosSagaPort {

    private final OutboxJpaRepository outbox;
    private final ObjectMapper objectMapper;

    @Override
    public void enviar(String destino, String tipo, String sagaId, Long ordemServicoId, Map<String, Object> dados) {
        Mensagem mensagem = Mensagem.nova(tipo, sagaId, ordemServicoId, objectMapper.valueToTree(dados));
        OutboxJpaEntity registro = new OutboxJpaEntity();
        registro.setId(mensagem.id());
        registro.setDestino(destino);
        registro.setTipo(tipo);
        registro.setSagaId(sagaId);
        try {
            registro.setConteudo(objectMapper.writeValueAsString(mensagem));
            registro.setCabecalhos(objectMapper.writeValueAsString(Rastreamento.cabecalhosAtuais(sagaId)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar o comando " + tipo, e);
        }
        registro.setCriadaEm(LocalDateTime.now());
        outbox.save(registro);
    }
}
