package br.com.fiap.challange.oficina.dto.response;

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
}
