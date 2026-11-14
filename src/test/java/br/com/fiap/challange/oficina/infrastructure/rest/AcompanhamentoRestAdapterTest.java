package br.com.fiap.challange.oficina.infrastructure.rest;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.rest.AcompanhamentoRestAdapter;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.rest.ServicosExternosProperties;
import br.com.fiap.challange.oficina.infrastructure.metrics.NegocioMetrics;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Sobe um servidor HTTP de verdade fazendo o papel do Billing/Execution para
 * conferir que o token e o correlation id são repassados e que o circuit
 * breaker abre quando o serviço começa a falhar.
 */
class AcompanhamentoRestAdapterTest {

    private HttpServer servidor;
    private final Map<String, String> cabecalhosRecebidos = new ConcurrentHashMap<>();
    private volatile int statusResposta = 200;
    private NegocioMetrics metrics;
    private AcompanhamentoRestAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", troca -> {
            cabecalhosRecebidos.put("auth", String.valueOf(troca.getRequestHeaders().getFirst("Authorization")));
            cabecalhosRecebidos.put("correlation", String.valueOf(troca.getRequestHeaders().getFirst("X-Correlation-Id")));
            byte[] corpo = "[{\"id\":\"x\",\"status\":\"PAGO\"}]".getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(statusResposta, corpo.length);
            try (OutputStream out = troca.getResponseBody()) {
                out.write(corpo);
            }
        });
        servidor.start();
        String url = "http://localhost:" + servidor.getAddress().getPort();

        ServicosExternosProperties properties = new ServicosExternosProperties();
        properties.setBillingUrl(url);
        properties.setExecucaoUrl(url);
        properties.setTimeoutMs(1000);
        CircuitBreakerRegistry circuitos = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1)).build());
        metrics = mock(NegocioMetrics.class);
        adapter = new AcompanhamentoRestAdapter(properties, RestClient.builder(), circuitos, metrics);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("correlationId", "admin:1234");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void repassaTokenECorrelationId() {
        assertThat(adapter.orcamentos(1L)).isPresent();
        assertThat(adapter.execucoes(1L)).isPresent();
        assertThat(cabecalhosRecebidos).containsEntry("auth", "Bearer abc").containsEntry("correlation", "admin:1234");
    }

    @Test
    void falhasAbremOCircuito() {
        statusResposta = 500;

        assertThat(adapter.orcamentos(1L)).isEmpty();
        assertThat(adapter.orcamentos(1L)).isEmpty();
        // circuito aberto: nem chega a chamar
        statusResposta = 200;
        assertThat(adapter.orcamentos(1L)).isEmpty();
        verify(metrics, org.mockito.Mockito.times(2)).registrarErroIntegracao("billing");
    }

    @Test
    void semRequisicaoHttpNaoQuebra() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
        assertThat(adapter.execucoes(1L)).isPresent();
        assertThat(cabecalhosRecebidos).containsEntry("auth", "null");
    }
}
