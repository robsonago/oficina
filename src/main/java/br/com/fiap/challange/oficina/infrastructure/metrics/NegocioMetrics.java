package br.com.fiap.challange.oficina.infrastructure.metrics;

import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Métricas de negócio expostas via Micrometer (/actuator/prometheus), para
 * os dashboards de volume/tempo médio/erros de integração.
 */
@Component
@RequiredArgsConstructor
public class NegocioMetrics {

    private final OrdemServicoRepositoryPort osRepository;
    private final MeterRegistry registry;

    @PostConstruct
    public void registrarGauges() {
        Gauge.builder("oficina.os.abertas.hoje", this, NegocioMetrics::contarAbertasHoje)
                .description("Ordens de serviço abertas no dia corrente")
                .register(registry);

        for (StatusOS status : StatusOS.values()) {
            Gauge.builder("oficina.os.tempo_medio_execucao.minutos", this,
                            metrics -> metrics.tempoMedioExecucaoPorStatus(status))
                    .tag("status", status.name())
                    .description("Tempo médio (minutos) entre início e finalização das OS, por status")
                    .register(registry);
        }
    }

    public void registrarErroIntegracao(String integracao) {
        registry.counter("oficina.integracoes.erros", "integracao", integracao).increment();
    }

    private double contarAbertasHoje() {
        LocalDate hoje = LocalDate.now();
        return osRepository.findAll().stream()
                .filter(os -> os.getDataAbertura() != null && os.getDataAbertura().toLocalDate().equals(hoje))
                .count();
    }

    private double tempoMedioExecucaoPorStatus(StatusOS status) {
        List<OrdemServico> comTempo = osRepository.findByStatus(status).stream()
                .filter(os -> os.getDataInicio() != null && os.getDataFinalizacao() != null)
                .toList();
        if (comTempo.isEmpty()) return 0;
        return comTempo.stream()
                .mapToLong(os -> ChronoUnit.MINUTES.between(os.getDataInicio(), os.getDataFinalizacao()))
                .average().orElse(0);
    }
}
