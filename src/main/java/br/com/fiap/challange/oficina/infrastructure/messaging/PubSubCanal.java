package br.com.fiap.challange.oficina.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(name = "mensageria.provider", havingValue = "pubsub")
public class PubSubCanal implements CanalMensageria {

    private final MensageriaProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
    private ManagedChannel canalEmulador;

    public PubSubCanal(MensageriaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enviar(String destino, Mensagem mensagem, Map<String, String> cabecalhos) {
        String topico = properties.topico(destino);
        try {
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(mensagem)))
                    .putAllAttributes(cabecalhos)
                    .putAttributes("tipo", mensagem.tipo())
                    .build();
            String messageId = publisher(topico).publish(pubsubMessage).get(10, TimeUnit.SECONDS);
            log.info("Mensagem {} publicada topico={} messageId={} saga={}", mensagem.tipo(), topico, messageId, mensagem.sagaId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaMensageriaException("Publicação interrompida no tópico " + topico, e);
        } catch (Exception e) {
            throw new FalhaMensageriaException("Não foi possível publicar no tópico " + topico, e);
        }
    }

    private Publisher publisher(String topico) {
        return publishers.computeIfAbsent(topico, nome -> {
            try {
                Publisher.Builder builder = Publisher.newBuilder(TopicName.of(properties.getProjectId(), nome));
                if (usaEmulador()) {
                    builder.setChannelProvider(canalEmulador())
                            .setCredentialsProvider(NoCredentialsProvider.create());
                }
                return builder.build();
            } catch (Exception e) {
                throw new FalhaMensageriaException("Não foi possível criar o publisher do tópico " + nome, e);
            }
        });
    }

    private boolean usaEmulador() {
        return properties.getEmuladorHost() != null && !properties.getEmuladorHost().isBlank();
    }

    private synchronized FixedTransportChannelProvider canalEmulador() {
        if (canalEmulador == null) {
            canalEmulador = ManagedChannelBuilder.forTarget(properties.getEmuladorHost()).usePlaintext().build();
        }
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(canalEmulador));
    }

    @PreDestroy
    public void encerrar() {
        publishers.values().forEach(Publisher::shutdown);
        if (canalEmulador != null) {
            canalEmulador.shutdown();
        }
    }
}
