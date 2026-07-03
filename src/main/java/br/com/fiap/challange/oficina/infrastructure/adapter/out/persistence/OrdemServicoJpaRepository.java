package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.OrdemServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, Long> {
    Optional<OrdemServicoJpaEntity> findByNumero(String numero);
    List<OrdemServicoJpaEntity> findByStatus(StatusOS status);
    List<OrdemServicoJpaEntity> findByStatusIn(List<StatusOS> statuses);
    List<OrdemServicoJpaEntity> findByClienteId(Long clienteId);
    List<OrdemServicoJpaEntity> findByVeiculoId(Long veiculoId);

    @Query("SELECT o FROM OrdemServicoJpaEntity o WHERE o.status IN ('FINALIZADA', 'ENTREGUE') AND o.dataInicio IS NOT NULL AND o.dataFinalizacao IS NOT NULL")
    List<OrdemServicoJpaEntity> findFinalizadasComTempo();
}
