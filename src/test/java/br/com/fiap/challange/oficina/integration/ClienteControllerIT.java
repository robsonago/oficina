package br.com.fiap.challange.oficina.integration;

import br.com.fiap.challange.oficina.dto.request.ClienteRequest;
import br.com.fiap.challange.oficina.model.Usuario;
import br.com.fiap.challange.oficina.repository.ClienteRepository;
import br.com.fiap.challange.oficina.repository.UsuarioRepository;
import br.com.fiap.challange.oficina.security.JwtService;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ClienteRepository clienteRepository;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();
        if (!usuarioRepository.existsByUsername("admin_it")) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin_it").password(passwordEncoder.encode("admin123"))
                    .role("ADMIN").ativo(true).build());
        }
        UserDetails userDetails = new User("admin_it", "admin123",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token = "Bearer " + jwtService.generateToken(userDetails);
    }

    @AfterEach
    void tearDown() {
        clienteRepository.deleteAll();
    }

    @Test
    void deveCriarClienteComSucesso() throws Exception {
        ClienteRequest request = new ClienteRequest(
                "Maria Oliveira", "529.982.247-25", "maria@email.com", "11999990000", "Rua B, 456");

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria Oliveira"))
                .andExpect(jsonPath("$.documento").value("52998224725"))
                .andExpect(jsonPath("$.tipoDocumento").value("CPF"));
    }

    @Test
    void deveRetornar400ParaCpfInvalido() throws Exception {
        ClienteRequest request = new ClienteRequest(
                "João Inválido", "111.111.111-11", null, null, null);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarClientes() throws Exception {
        mockMvc.perform(get("/api/clientes")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404QuandoClienteNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/clientes/99999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarEBuscarClientePorDocumento() throws Exception {
        ClienteRequest request = new ClienteRequest(
                "Pedro Santos", "111.444.777-35", "pedro@email.com", null, null);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/clientes/documento/11144477735")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pedro Santos"));
    }

    @Test
    void deveRejeitarCriacaoSemNome() throws Exception {
        ClienteRequest request = new ClienteRequest(null, "529.982.247-25", null, null, null);

        mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarCliente() throws Exception {
        ClienteRequest criar = new ClienteRequest("João Antigo", "529.982.247-25", null, null, null);
        String result = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criar)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(result).get("id").asLong();
        ClienteRequest atualizar = new ClienteRequest("João Atualizado", "529.982.247-25", null, null, null);

        mockMvc.perform(put("/api/clientes/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atualizar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Atualizado"));
    }

    @Test
    void deveDeletarCliente() throws Exception {
        ClienteRequest criar = new ClienteRequest("Ana Lima", "111.444.777-35", null, null, null);
        String result = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criar)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(result).get("id").asLong();
        mockMvc.perform(delete("/api/clientes/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }
}
