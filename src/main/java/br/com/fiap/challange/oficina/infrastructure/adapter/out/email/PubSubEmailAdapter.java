package br.com.fiap.challange.oficina.infrastructure.adapter.out.email;

import br.com.fiap.challange.oficina.domain.port.out.EmailPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Em vez de enviar o e-mail diretamente (síncrono), publica um evento no
 * Pub/Sub e segue — uma function serverless (repositório oficina-auth-function)
 * é quem escuta esse tópico e manda o e-mail de fato.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notifications.pubsub.topic")
public class PubSubEmailAdapter implements EmailPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Publisher publisher;

    public PubSubEmailAdapter(@Value("${notifications.pubsub.project-id}") String projectId,
                               @Value("${notifications.pubsub.topic}") String topic) throws Exception {
        this.publisher = Publisher.newBuilder(TopicName.of(projectId, topic)).build();
    }

    @Override
    public void enviarNotificacaoOrcamento(String destinatario, String nomeCliente,
                                           String numeroOS, BigDecimal valorTotal) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Notificação não publicada para OS {}: cliente sem e-mail cadastrado", numeroOS);
            return;
        }

        ObjectNode evento = MAPPER.createObjectNode();
        evento.put("tipo", "ORCAMENTO_DISPONIVEL");
        evento.put("destinatario", destinatario);
        evento.put("nomeCliente", nomeCliente);
        evento.put("numeroOS", numeroOS);
        evento.put("valorTotal", valorTotal.toPlainString());

        PubsubMessage message;
        try {
            message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(MAPPER.writeValueAsString(evento)))
                    .build();
        } catch (Exception e) {
            log.error("Falha ao serializar evento de notificação para={} OS={}: {}", destinatario, numeroOS, e.getMessage());
            return;
        }

        try {
            publisher.publish(message).get();
            log.info("Evento de notificação publicado para={} OS={}", destinatario, numeroOS);
        } catch (Exception e) {
            log.error("Falha ao publicar evento de notificação para={} OS={}: {}", destinatario, numeroOS, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        publisher.shutdown();
    }
}
