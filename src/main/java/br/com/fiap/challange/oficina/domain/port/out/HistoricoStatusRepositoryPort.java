package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.HistoricoStatusOS;

import java.util.List;

public interface HistoricoStatusRepositoryPort {
    void registrar(HistoricoStatusOS historico);
    List<HistoricoStatusOS> buscarPorOrdemServico(Long ordemServicoId);
}
