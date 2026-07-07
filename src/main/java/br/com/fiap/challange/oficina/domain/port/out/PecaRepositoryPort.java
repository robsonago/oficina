package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.Peca;

import java.util.List;
import java.util.Optional;

public interface PecaRepositoryPort {
    Peca save(Peca peca);
    Optional<Peca> findById(Long id);
    List<Peca> findByAtivoTrue();
    void deleteAll();
}
