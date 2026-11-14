package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.HistoricoStatusJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface HistoricoStatusJpaRepository extends JpaRepository<HistoricoStatusJpaEntity, Long> {
    List<HistoricoStatusJpaEntity> findByOrdemServicoIdOrderByOcorridoEmAscIdAsc(Long ordemServicoId);
}
