package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepositoryPort {
    Cliente save(Cliente cliente);
    Optional<Cliente> findById(Long id);
    Optional<Cliente> findByDocumento(String documento);
    boolean existsByDocumento(String documento);
    List<Cliente> findAll();
    void deleteAll();
}
