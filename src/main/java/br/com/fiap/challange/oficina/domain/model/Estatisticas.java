package br.com.fiap.challange.oficina.domain.model;

public record Estatisticas(
        Long tempoMedioExecucaoMinutos,
        Integer totalOSFinalizadas,
        Long totalRecebida,
        Long totalEmDiagnostico,
        Long totalAguardandoAprovacao,
        Long totalEmExecucao,
        Long totalFinalizada,
        Long totalEntregue
) {}
