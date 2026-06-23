package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.Veiculo;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VeiculoJpaRepository extends JpaRepository<Veiculo, Long>, VeiculoRepositoryPort {
    Optional<Veiculo> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<Veiculo> findByClienteId(Long clienteId);
}
