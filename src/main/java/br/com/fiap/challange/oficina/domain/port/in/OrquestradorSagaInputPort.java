package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.SagaOrdemServico;
import br.com.fiap.challange.oficina.domain.port.in.command.EventoSagaCommand;

import java.util.List;

public interface OrquestradorSagaInputPort {
    SagaOrdemServico iniciar(Long ordemServicoId, String solicitante);
    void tratar(EventoSagaCommand evento);
    int verificarPrazos();
    List<SagaOrdemServico> sagasDaOrdemServico(Long ordemServicoId);
}
