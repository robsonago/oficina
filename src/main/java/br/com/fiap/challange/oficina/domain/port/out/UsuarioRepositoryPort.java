package br.com.fiap.challange.oficina.domain.port.out;

import br.com.fiap.challange.oficina.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {
    Usuario save(Usuario usuario);
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
}
