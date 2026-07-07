package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.VeiculoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface VeiculoJpaRepository extends JpaRepository<VeiculoJpaEntity, Long> {
    Optional<VeiculoJpaEntity> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<VeiculoJpaEntity> findByClienteId(Long clienteId);
}
