package br.com.fiap.challange.oficina.infrastructure.config;

import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByUsername("admin")) {
            return;
        }
        Usuario admin = Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role("ADMIN")
                .ativo(true)
                .build();
        try {
            usuarioRepository.save(admin);
            log.info("Usuário admin criado com sucesso. Senha padrão: admin123");
        } catch (DataIntegrityViolationException e) {
            // Outra réplica venceu a corrida (mesmo check-then-act em paralelo no boot) e já criou
            // o admin entre o existsByUsername acima e este save — não é um erro real.
            log.info("Usuário admin já foi criado por outra réplica concorrente, ignorando.");
        }
    }
}
