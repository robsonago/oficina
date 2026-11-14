package br.com.fiap.challange.oficina.infrastructure.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "mensageria")
public class MensageriaProperties {

    /** "pubsub" em nuvem/docker-compose, "memoria" nos testes. */
    private String provider = "memoria";

    private String projectId;

    /** Host do emulador do Pub/Sub (ex.: localhost:8085). Vazio = Pub/Sub de verdade. */
    private String emuladorHost;

    /** Nome lógico do destino -> nome real do tópico. */
    private Map<String, String> topicos = new HashMap<>();

    /** Assinatura de onde este serviço consome os comandos que recebe. */
    private String assinatura;

    public String topico(String destino) {
        String topico = topicos.get(destino);
        if (topico == null || topico.isBlank()) {
            throw new IllegalStateException("Tópico não configurado para o destino '" + destino + "'");
        }
        return topico;
    }
}
