package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;

import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepositoryPort {
    OrdemServico save(OrdemServico ordemServico);
    Optional<OrdemServico> findById(Long id);
    Optional<OrdemServico> findByNumero(String numero);
    List<OrdemServico> findAll();
    List<OrdemServico> findByStatus(StatusOS status);
    List<OrdemServico> findByStatusIn(List<StatusOS> statuses);
    List<OrdemServico> findByClienteId(Long clienteId);
    List<OrdemServico> findByVeiculoId(Long veiculoId);
    List<OrdemServico> findFinalizadasComTempo();
    void deleteAll();
}
