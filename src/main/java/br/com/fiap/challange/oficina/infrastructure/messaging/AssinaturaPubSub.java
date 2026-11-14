package br.com.fiap.challange.oficina.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.newrelic.api.agent.Trace;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consome a assinatura do serviço no Pub/Sub. Só sobe depois que a
 * aplicação está pronta, para não receber mensagem com o contexto ainda
 * carregando.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mensageria.provider", havingValue = "pubsub")
public class AssinaturaPubSub {

    private final MensageriaProperties properties;
    private final ObjectMapper objectMapper;
    private final ReceptorMensagens receptor;
    private Subscriber subscriber;
    private ManagedChannel canalEmulador;

    public AssinaturaPubSub(MensageriaProperties properties, ObjectMapper objectMapper, ReceptorMensagens receptor) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.receptor = receptor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        ProjectSubscriptionName nome = ProjectSubscriptionName.of(properties.getProjectId(), properties.getAssinatura());
        Subscriber.Builder builder = Subscriber.newBuilder(nome, this::processar);
        if (properties.getEmuladorHost() != null && !properties.getEmuladorHost().isBlank()) {
            canalEmulador = ManagedChannelBuilder.forTarget(properties.getEmuladorHost()).usePlaintext().build();
            builder.setChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(canalEmulador)))
                    .setCredentialsProvider(NoCredentialsProvider.create());
        }
        subscriber = builder.build();
        subscriber.startAsync().awaitRunning();
        log.info("Escutando a assinatura {}", nome);
    }

    @Trace(dispatcher = true)
    void processar(PubsubMessage message, AckReplyConsumer consumer) {
        String tipo = message.getAttributesOrDefault("tipo", "DESCONHECIDO");
        try {
            Rastreamento.continuar(message.getAttributesMap(), "consumir/" + tipo);
            Mensagem mensagem = objectMapper.readValue(message.getData().toStringUtf8(), Mensagem.class);
            receptor.receber(mensagem);
            consumer.ack();
        } catch (Exception e) {
            log.error("Falha ao processar mensagem {} id={}: {}", tipo, message.getMessageId(), e.getMessage(), e);
            consumer.nack();
        } finally {
            Rastreamento.limpar();
        }
    }

    @PreDestroy
    public void encerrar() {
        if (subscriber != null) {
            subscriber.stopAsync();
        }
        if (canalEmulador != null) {
            canalEmulador.shutdown();
        }
    }
}
