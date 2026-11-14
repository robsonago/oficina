package br.com.fiap.challange.oficina.infrastructure.adapter.out.mensageria;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.OutboxJpaEntity;
import br.com.fiap.challange.oficina.infrastructure.messaging.CanalMensageria;
import br.com.fiap.challange.oficina.infrastructure.messaging.Mensagem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Publica no Pub/Sub o que ficou pendente na outbox. Se o broker estiver
 * fora, a mensagem continua na tabela e é tentada de novo no próximo ciclo.
 * Com mais de uma réplica a mesma mensagem pode sair duas vezes — tudo bem,
 * os consumidores são idempotentes pelo id da mensagem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelayOutbox {

    private static final int LOTE = 50;

    private final OutboxJpaRepository outbox;
    private final CanalMensageria canal;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${mensageria.outbox.intervalo-ms:1000}")
    public void publicarPendentes() {
        for (OutboxJpaEntity registro : outbox.findByPublicadaEmIsNullOrderByCriadaEmAsc(Limit.of(LOTE))) {
            try {
                Mensagem mensagem = objectMapper.readValue(registro.getConteudo(), Mensagem.class);
                Map<String, String> cabecalhos = registro.getCabecalhos() == null ? Map.of()
                        : objectMapper.readValue(registro.getCabecalhos(), new TypeReference<>() {
                        });
                canal.enviar(registro.getDestino(), mensagem, cabecalhos);
                registro.setPublicadaEm(LocalDateTime.now());
                outbox.save(registro);
            } catch (Exception e) {
                registro.setTentativas(registro.getTentativas() + 1);
                outbox.save(registro);
                log.warn("Falha ao publicar {} da outbox (tentativa {}): {}", registro.getTipo(),
                        registro.getTentativas(), e.getMessage());
                // para o lote para não embaralhar a ordem dos eventos da mesma saga
                return;
            }
        }
    }
}
