package br.com.fiap.challange.oficina.integration;

import br.com.fiap.challange.oficina.dto.request.ServicoRequest;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.out.ServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServicoControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ServicoRepositoryPort servicoRepository;
    @Autowired
    UsuarioRepositoryPort usuarioRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setUp() {
        servicoRepository.deleteAll();

        if (!usuarioRepository.existsByUsername("admin_servico_it")) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin_servico_it").password(passwordEncoder.encode("admin123"))
                    .role("ADMIN").ativo(true).build());
        }
        UserDetails userDetails = new User("admin_servico_it", "admin123",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token = "Bearer " + jwtService.generateToken(userDetails);
    }

    @AfterEach
    void tearDown() {
        servicoRepository.deleteAll();
    }

    @Test
    void deveCriarServicoComSucesso() throws Exception {
        ServicoRequest request = new ServicoRequest("Troca de óleo", "Troca completa com filtro", new BigDecimal("150.00"), 60);

        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Troca de óleo"))
                .andExpect(jsonPath("$.preco").value(150.00));
    }

    @Test
    void deveListarServicos() throws Exception {
        mockMvc.perform(get("/api/servicos")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveBuscarServicoPorId() throws Exception {
        String json = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServicoRequest("Alinhamento", null, new BigDecimal("80.00"), 45))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(get("/api/servicos/" + id)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alinhamento"));
    }

    @Test
    void deveAtualizarServico() throws Exception {
        String json = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServicoRequest("Balanceamento", null, new BigDecimal("60.00"), 30))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(put("/api/servicos/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServicoRequest("Balanceamento Premium", null, new BigDecimal("75.00"), 40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Balanceamento Premium"))
                .andExpect(jsonPath("$.preco").value(75.00));
    }

    @Test
    void deveDeletarServico() throws Exception {
        String json = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServicoRequest("Revisão geral", null, new BigDecimal("200.00"), 120))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(delete("/api/servicos/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar404QuandoServicoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/servicos/99999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ParaPrecoInvalido() throws Exception {
        ServicoRequest request = new ServicoRequest("Serviço Inválido", null, new BigDecimal("0.00"), 30);

        mockMvc.perform(post("/api/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/servicos"))
                .andExpect(status().isUnauthorized());
    }
}
