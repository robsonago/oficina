package br.com.fiap.challange.oficina.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercita os adaptadores reais do Pub/Sub contra o emulador oficial do
 * Google — o mesmo usado no docker-compose. Garante que o envelope, os
 * atributos de rastreamento e o ack/nack funcionam fora dos mocks.
 */
@Testcontainers
class PubSubEmuladorIT {

    private static final String PROJETO = "oficina-teste";

    @Container
    static PubSubEmulatorContainer emulador = new PubSubEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators"));

    private static ManagedChannel canal;
    private static TransportChannelProvider transporte;
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @BeforeAll
    static void criarTopicos() throws Exception {
        canal = ManagedChannelBuilder.forTarget(emulador.getEmulatorEndpoint()).usePlaintext().build();
        transporte = FixedTransportChannelProvider.create(GrpcTransportChannel.create(canal));
        try (TopicAdminClient topicos = TopicAdminClient.create(TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(transporte).setCredentialsProvider(NoCredentialsProvider.create()).build());
             SubscriptionAdminClient assinaturas = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
                     .setTransportChannelProvider(transporte).setCredentialsProvider(NoCredentialsProvider.create()).build())) {
            for (String nome : new String[]{"saida", "entrada"}) {
                topicos.createTopic(TopicName.of(PROJETO, nome));
                assinaturas.createSubscription(SubscriptionName.of(PROJETO, nome + "-sub"), TopicName.of(PROJETO, nome),
                        PushConfig.getDefaultInstance(), 10);
            }
        }
    }

    @AfterAll
    static void fechar() {
        canal.shutdownNow();
    }

    private MensageriaProperties properties() {
        MensageriaProperties properties = new MensageriaProperties();
        properties.setProvider("pubsub");
        properties.setProjectId(PROJETO);
        properties.setEmuladorHost(emulador.getEmulatorEndpoint());
        properties.setTopicos(Map.of("saida", "saida", "entrada", "entrada"));
        properties.setAssinatura("entrada-sub");
        return properties;
    }

    @Test
    void publicaComEnvelopeEAtributos() throws Exception {
        PubSubCanal publicador = new PubSubCanal(properties(), MAPPER);
        Mensagem mensagem = Mensagem.nova("TESTE_SAIDA", "saga-1", 7L, MAPPER.createObjectNode().put("valor", 10));

        publicador.enviar("saida", mensagem, Map.of("correlationId", "abc-123", "sagaId", "saga-1"));
        publicador.encerrar();

        try (SubscriptionAdminClient assinaturas = SubscriptionAdminClient.create(SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(transporte).setCredentialsProvider(NoCredentialsProvider.create()).build())) {
            PullResponse resposta = assinaturas.pull(SubscriptionName.of(PROJETO, "saida-sub"), 1);
            ReceivedMessage recebida = resposta.getReceivedMessages(0);
            Mensagem lida = MAPPER.readValue(recebida.getMessage().getData().toStringUtf8(), Mensagem.class);

            assertThat(lida.id()).isEqualTo(mensagem.id());
            assertThat(lida.dados().path("valor").asInt()).isEqualTo(10);
            assertThat(recebida.getMessage().getAttributesMap())
                    .containsEntry("tipo", "TESTE_SAIDA")
                    .containsEntry("correlationId", "abc-123");
        }
    }

    @Test
    void consomeEDevolveParaAFilaQuandoOProcessamentoFalha() throws Exception {
        BlockingQueue<Mensagem> recebidas = new LinkedBlockingQueue<>();
        AtomicBoolean primeiraTentativa = new AtomicBoolean(true);
        ReceptorMensagens receptor = mensagem -> {
            if ("FALHA_UMA_VEZ".equals(mensagem.tipo()) && primeiraTentativa.getAndSet(false)) {
                throw new IllegalStateException("simulando banco fora do ar");
            }
            recebidas.add(mensagem);
        };
        AssinaturaPubSub assinatura = new AssinaturaPubSub(properties(), MAPPER, receptor);
        assinatura.iniciar();
        PubSubCanal publicador = new PubSubCanal(properties(), MAPPER);
        try {
            publicador.enviar("entrada", Mensagem.nova("FALHA_UMA_VEZ", "saga-2", 8L, MAPPER.createObjectNode()),
                    Rastreamento.cabecalhosAtuais("saga-2"));

            // nack na primeira tentativa, o Pub/Sub reentrega e aí processa
            Mensagem processada = recebidas.poll(30, TimeUnit.SECONDS);
            assertThat(processada).isNotNull();
            assertThat(processada.sagaId()).isEqualTo("saga-2");
            assertThat(primeiraTentativa.get()).isFalse();
        } finally {
            publicador.encerrar();
            assinatura.encerrar();
        }
    }

    @Test
    void destinoSemTopicoConfigurado() {
        PubSubCanal publicador = new PubSubCanal(properties(), MAPPER);
        Mensagem mensagem = Mensagem.nova("X", "s", 1L, MAPPER.createObjectNode());

        assertThatThrownBy(() -> publicador.enviar("nao-existe", mensagem, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nao-existe");
    }
}
