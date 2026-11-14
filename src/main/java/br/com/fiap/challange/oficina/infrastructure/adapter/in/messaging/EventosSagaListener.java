package br.com.fiap.challange.oficina.infrastructure.adapter.in.messaging;

import br.com.fiap.challange.oficina.domain.port.in.OrquestradorSagaInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.EventoSagaCommand;
import br.com.fiap.challange.oficina.domain.port.out.MensagensProcessadasPort;
import br.com.fiap.challange.oficina.infrastructure.messaging.Mensagem;
import br.com.fiap.challange.oficina.infrastructure.messaging.ReceptorMensagens;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Entrada dos eventos que o Billing e o Execution Service publicam em
 * resposta aos comandos da saga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventosSagaListener implements ReceptorMensagens {

    private final OrquestradorSagaInputPort orquestrador;
    private final MensagensProcessadasPort processadas;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void receber(Mensagem mensagem) {
        if (processadas.jaProcessada(mensagem.id())) {
            log.info("Evento {} ({}) repetido, ignorando", mensagem.id(), mensagem.tipo());
            return;
        }
        log.info("Evento {} recebido saga={} os={}", mensagem.tipo(), mensagem.sagaId(), mensagem.ordemServicoId());
        Map<String, Object> dados = mensagem.dados() == null ? Map.of()
                : objectMapper.convertValue(mensagem.dados(), new TypeReference<>() {
                });
        orquestrador.tratar(new EventoSagaCommand(mensagem.tipo(), mensagem.sagaId(), mensagem.ordemServicoId(), dados));
        processadas.registrar(mensagem.id(), mensagem.tipo());
    }
}
