package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.Servico;

import java.util.List;
import java.util.Optional;

public interface ServicoRepositoryPort {
    Servico save(Servico servico);
    Optional<Servico> findById(Long id);
    List<Servico> findByAtivoTrue();
    void deleteAll();
}
