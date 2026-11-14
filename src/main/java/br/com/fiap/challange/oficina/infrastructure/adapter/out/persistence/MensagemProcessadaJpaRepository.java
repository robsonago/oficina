package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.MensagemProcessadaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface MensagemProcessadaJpaRepository extends JpaRepository<MensagemProcessadaJpaEntity, String> {
}
