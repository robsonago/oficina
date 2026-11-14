package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.SagaOrdemServico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SagaRepositoryPort {
    SagaOrdemServico salvar(SagaOrdemServico saga);
    Optional<SagaOrdemServico> buscarPorId(String id);
    List<SagaOrdemServico> buscarPorOrdemServico(Long ordemServicoId);
    Optional<SagaOrdemServico> buscarAtivaPorOrdemServico(Long ordemServicoId);
    List<SagaOrdemServico> buscarComPrazoVencido(LocalDateTime agora);
}
