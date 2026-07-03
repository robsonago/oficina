package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.ItemPecaOSJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemPecaOSJpaRepository extends JpaRepository<ItemPecaOSJpaEntity, Long> {
}
