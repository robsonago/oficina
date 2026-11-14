package br.com.fiap.challange.oficina.infrastructure.adapter.in.scheduler;

import br.com.fiap.challange.oficina.domain.port.in.OrquestradorSagaInputPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sagas paradas esperando o cliente (aprovar ou pagar) ou o Billing (gerar o
 * orçamento) não podem ficar abertas para sempre segurando peças do estoque.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "saga.verificar-prazos", havingValue = "true", matchIfMissing = true)
public class VerificadorPrazosSaga {

    private final OrquestradorSagaInputPort orquestrador;

    @Scheduled(fixedDelayString = "${saga.intervalo-verificacao-ms:60000}", initialDelay = 30000)
    public void verificar() {
        try {
            int vencidas = orquestrador.verificarPrazos();
            if (vencidas > 0) {
                log.info("{} saga(s) com prazo vencido foram compensadas", vencidas);
            }
        } catch (Exception e) {
            // outra réplica pode ter tratado a mesma saga (lock otimista); tenta de novo no próximo ciclo
            log.warn("Verificação de prazos da saga falhou: {}", e.getMessage());
        }
    }
}
