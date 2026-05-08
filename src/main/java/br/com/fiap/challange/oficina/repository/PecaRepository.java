package br.com.fiap.challange.oficina.repository;

import br.com.fiap.challange.oficina.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PecaRepository extends JpaRepository<Peca, Long> {
    List<Peca> findByAtivoTrue();
}
