package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServico, Long>, OrdemServicoRepositoryPort {
    Optional<OrdemServico> findByNumero(String numero);
    List<OrdemServico> findByStatus(StatusOS status);
    List<OrdemServico> findByStatusIn(List<StatusOS> statuses);
    List<OrdemServico> findByClienteId(Long clienteId);
    List<OrdemServico> findByVeiculoId(Long veiculoId);

    @Query("SELECT o FROM OrdemServico o WHERE o.status IN ('FINALIZADA', 'ENTREGUE') AND o.dataInicio IS NOT NULL AND o.dataFinalizacao IS NOT NULL")
    List<OrdemServico> findFinalizadasComTempo();
}
