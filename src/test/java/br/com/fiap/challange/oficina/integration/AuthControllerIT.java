package br.com.fiap.challange.oficina.integration;

import br.com.fiap.challange.oficina.dto.request.LoginRequest;
import br.com.fiap.challange.oficina.dto.request.UsuarioRequest;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UsuarioRepositoryPort usuarioRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() {
        if (!usuarioRepository.existsByUsername("admin_auth_it")) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin_auth_it").password(passwordEncoder.encode("admin123"))
                    .role("ADMIN").ativo(true).build());
        }
        adminToken = "Bearer " + jwtService.generateToken("admin_auth_it", "ADMIN");
    }

    @Test
    void deveRealizarLoginComSucesso() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin_auth_it", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin_auth_it"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void deveRetornar401ParaSenhaInvalida() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin_auth_it", "senha_errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar401ParaUsuarioInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("usuario_inexistente", "qualquersenha"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRegistrarNovoUsuarioComoAdmin() throws Exception {
        String novoUsername = "tecnico_novo_" + System.currentTimeMillis();
        UsuarioRequest request = new UsuarioRequest(novoUsername, "senha123", "TECNICO");

        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void deveRetornar403AoRegistrarSemPermissaoAdmin() throws Exception {
        String tecnicoToken = "Bearer " + jwtService.generateToken("tecnico_sem_permissao", "TECNICO");

        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", tecnicoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest("novo_user", "senha123", "TECNICO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar400ParaUsuarioDuplicado() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest("admin_auth_it", "senha123", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ParaLoginSemUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"admin123\"}"))
                .andExpect(status().isBadRequest());
    }
}
