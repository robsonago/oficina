package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.SagaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface SagaJpaRepository extends JpaRepository<SagaJpaEntity, String> {
    List<SagaJpaEntity> findByOrdemServicoIdOrderByCriadaEmDesc(Long ordemServicoId);
    Optional<SagaJpaEntity> findFirstByOrdemServicoIdAndEtapaNotInOrderByCriadaEmDesc(Long ordemServicoId,
                                                                                       Collection<EtapaSaga> encerradas);
    List<SagaJpaEntity> findByEtapaInAndPrazoBefore(Collection<EtapaSaga> etapas, LocalDateTime agora);
}
