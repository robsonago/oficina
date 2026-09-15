package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.config.DataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dataInitializer = new DataInitializer(usuarioRepository, passwordEncoder);
    }

    @Test
    void naoCriaAdminQuandoJaExiste() throws Exception {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        dataInitializer.run();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void criaAdminQuandoNaoExiste() throws Exception {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hash");

        dataInitializer.run();

        verify(usuarioRepository).save(argThat((Usuario u) ->
                "admin".equals(u.getUsername()) && "ADMIN".equals(u.getRole()) && u.getAtivo()));
    }

    @Test
    void ignoraCorridaEntreReplicasConcorrentes() throws Exception {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hash");
        when(usuarioRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        dataInitializer.run();

        verify(usuarioRepository).save(any());
    }
}
