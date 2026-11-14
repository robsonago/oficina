package br.com.fiap.challange.oficina.infrastructure.messaging;

import com.newrelic.api.agent.ConcurrentHashMapHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransportType;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Propaga o contexto de rastreamento entre serviços através dos atributos
 * da mensagem: o correlationId (que já aparece nos logs) e os cabeçalhos de
 * distributed tracing da New Relic (newrelic/traceparent/tracestate).
 * Sem o agente rodando, as chamadas à API da New Relic não fazem nada.
 */
public final class Rastreamento {

    public static final String CORRELATION_ID = "correlationId";
    public static final String SAGA_ID = "sagaId";

    private Rastreamento() {
    }

    public static Map<String, String> cabecalhosAtuais(String sagaId) {
        Map<String, String> cabecalhos = new HashMap<>();
        Headers headers = ConcurrentHashMapHeaders.build(HeaderType.MESSAGE);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        for (String nome : headers.getHeaderNames()) {
            cabecalhos.put(nome, headers.getHeader(nome));
        }
        String correlationId = MDC.get(CORRELATION_ID);
        cabecalhos.put(CORRELATION_ID, correlationId != null ? correlationId : "msg:" + UUID.randomUUID().toString().substring(0, 8));
        if (sagaId != null) {
            cabecalhos.put(SAGA_ID, sagaId);
        }
        return cabecalhos;
    }

    public static void continuar(Map<String, String> atributos, String nomeTransacao) {
        NewRelic.setTransactionName("Mensageria", nomeTransacao);
        Headers headers = ConcurrentHashMapHeaders.buildFromFlatMap(HeaderType.MESSAGE, new HashMap<>(atributos));
        NewRelic.getAgent().getTransaction().acceptDistributedTraceHeaders(TransportType.Other, headers);
        if (atributos.containsKey(CORRELATION_ID)) {
            MDC.put(CORRELATION_ID, atributos.get(CORRELATION_ID));
        }
        if (atributos.containsKey(SAGA_ID)) {
            MDC.put(SAGA_ID, atributos.get(SAGA_ID));
            NewRelic.addCustomParameter(SAGA_ID, atributos.get(SAGA_ID));
        }
    }

    public static void limpar() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(SAGA_ID);
    }
}
