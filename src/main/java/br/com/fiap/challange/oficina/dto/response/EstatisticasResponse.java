package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.Estatisticas;

public record EstatisticasResponse(
        Long tempoMedioExecucaoMinutos,
        Integer totalOSFinalizadas,
        Long totalOSPorStatus_recebida,
        Long totalOSPorStatus_emDiagnostico,
        Long totalOSPorStatus_aguardandoAprovacao,
        Long totalOSPorStatus_emExecucao,
        Long totalOSPorStatus_finalizada,
        Long totalOSPorStatus_entregue
) {
    public static EstatisticasResponse from(Estatisticas e) {
        return new EstatisticasResponse(
                e.tempoMedioExecucaoMinutos(), e.totalOSFinalizadas(),
                e.totalRecebida(), e.totalEmDiagnostico(),
                e.totalAguardandoAprovacao(), e.totalEmExecucao(),
                e.totalFinalizada(), e.totalEntregue());
    }
}
