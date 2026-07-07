package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.PecaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface PecaJpaRepository extends JpaRepository<PecaJpaEntity, Long> {
    List<PecaJpaEntity> findByAtivoTrue();
}
