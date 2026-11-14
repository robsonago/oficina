package br.com.fiap.challange.oficina.infrastructure.adapter.out.rest;

import br.com.fiap.challange.oficina.domain.port.out.AcompanhamentoExternoPort;
import br.com.fiap.challange.oficina.infrastructure.metrics.NegocioMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Chamadas REST síncronas para o Billing e o Execution Service.
 * Repassa o token de quem chamou (cada serviço valida o JWT de novo) e o
 * X-Correlation-Id. Timeout curto + circuit breaker: se um serviço estiver
 * fora, a consulta devolve o que tem em vez de travar.
 */
@Slf4j
@Component
public class AcompanhamentoRestAdapter implements AcompanhamentoExternoPort {

    private final RestClient billing;
    private final RestClient execucao;
    private final CircuitBreaker circuitoBilling;
    private final CircuitBreaker circuitoExecucao;
    private final NegocioMetrics metrics;

    public AcompanhamentoRestAdapter(ServicosExternosProperties properties, RestClient.Builder builder,
                                     CircuitBreakerRegistry circuitos, NegocioMetrics metrics) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()));
        fabrica.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));
        this.billing = builder.clone().baseUrl(properties.getBillingUrl()).requestFactory(fabrica).build();
        this.execucao = builder.clone().baseUrl(properties.getExecucaoUrl()).requestFactory(fabrica).build();
        this.circuitoBilling = circuitos.circuitBreaker("billing");
        this.circuitoExecucao = circuitos.circuitBreaker("execucao");
        this.metrics = metrics;
    }

    @Override
    public Optional<Object> orcamentos(Long ordemServicoId) {
        return chamar("billing", circuitoBilling, () -> billing.get()
                .uri("/api/orcamentos/ordem-servico/{id}", ordemServicoId)
                .headers(this::repassarCabecalhos)
                .retrieve().body(JsonNode.class));
    }

    @Override
    public Optional<Object> execucoes(Long ordemServicoId) {
        return chamar("execucao", circuitoExecucao, () -> execucao.get()
                .uri("/api/execucoes/ordem-servico/{id}", ordemServicoId)
                .headers(this::repassarCabecalhos)
                .retrieve().body(JsonNode.class));
    }

    private Optional<Object> chamar(String servico, CircuitBreaker circuito, Supplier<JsonNode> chamada) {
        try {
            return Optional.ofNullable(circuito.executeSupplier(chamada));
        } catch (CallNotPermittedException e) {
            log.warn("Circuito do {} aberto, consulta não realizada", servico);
        } catch (Exception e) {
            log.warn("Falha ao consultar {}: {}", servico, e.getMessage());
            metrics.registrarErroIntegracao(servico);
        }
        return Optional.empty();
    }

    private void repassarCabecalhos(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest request = atributos.getRequest();
            String autorizacao = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (autorizacao != null) {
                headers.set(HttpHeaders.AUTHORIZATION, autorizacao);
            }
        }
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.set("X-Correlation-Id", correlationId);
        }
    }
}
