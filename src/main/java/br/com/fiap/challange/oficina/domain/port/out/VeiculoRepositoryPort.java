package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.Veiculo;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepositoryPort {
    Veiculo save(Veiculo veiculo);
    Optional<Veiculo> findById(Long id);
    Optional<Veiculo> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<Veiculo> findAll();
    List<Veiculo> findByClienteId(Long clienteId);
    void deleteAll();
}
