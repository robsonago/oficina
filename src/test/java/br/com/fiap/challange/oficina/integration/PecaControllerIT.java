package br.com.fiap.challange.oficina.integration;

import br.com.fiap.challange.oficina.dto.request.AtualizarEstoqueRequest;
import br.com.fiap.challange.oficina.dto.request.PecaRequest;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
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
class PecaControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PecaRepositoryPort pecaRepository;
    @Autowired
    UsuarioRepositoryPort usuarioRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setUp() {
        pecaRepository.deleteAll();

        if (!usuarioRepository.existsByUsername("admin_peca_it")) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin_peca_it").password(passwordEncoder.encode("admin123"))
                    .role("ADMIN").ativo(true).build());
        }
        UserDetails userDetails = new User("admin_peca_it", "admin123",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token = "Bearer " + jwtService.generateToken(userDetails);
    }

    @AfterEach
    void tearDown() {
        pecaRepository.deleteAll();
    }

    @Test
    void deveCriarPecaComSucesso() throws Exception {
        PecaRequest request = new PecaRequest("Filtro de óleo", "Filtro Mann original", new BigDecimal("45.90"), 10, "FILT-001");

        mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Filtro de óleo"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(10));
    }

    @Test
    void deveListarPecas() throws Exception {
        mockMvc.perform(get("/api/pecas")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveAtualizarPeca() throws Exception {
        String json = mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("Vela de ignição", null, new BigDecimal("25.00"), 20, "VELA-001"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(put("/api/pecas/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("Vela de ignição NGK", null, new BigDecimal("30.00"), 15, "VELA-NGK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Vela de ignição NGK"))
                .andExpect(jsonPath("$.precoUnitario").value(30.00));
    }

    @Test
    void deveAtualizarEstoque() throws Exception {
        String json = mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("Pastilha de freio", null, new BigDecimal("80.00"), 5, "PAST-001"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(patch("/api/pecas/" + id + "/estoque")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AtualizarEstoqueRequest(50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(50));
    }

    @Test
    void deveDeletarPeca() throws Exception {
        String json = mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("Correia dentada", null, new BigDecimal("120.00"), 3, "CORR-001"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(delete("/api/pecas/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar404QuandoPecaNaoEncontrada() throws Exception {
        mockMvc.perform(put("/api/pecas/99999")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("X", null, new BigDecimal("10.00"), 1, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ParaNomeEmBranco() throws Exception {
        PecaRequest request = new PecaRequest("", null, new BigDecimal("10.00"), 5, null);

        mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/pecas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveBuscarPecaPorId() throws Exception {
        String json = mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("Amortecedor", null, new BigDecimal("350.00"), 2, "AMOR-001"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(get("/api/pecas/" + id)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Amortecedor"));
    }
}
