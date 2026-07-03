package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.ServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface ServicoJpaRepository extends JpaRepository<ServicoJpaEntity, Long> {
    List<ServicoJpaEntity> findByAtivoTrue();
}
