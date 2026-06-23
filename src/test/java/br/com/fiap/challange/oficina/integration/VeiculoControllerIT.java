package br.com.fiap.challange.oficina.integration;

import br.com.fiap.challange.oficina.dto.request.ClienteRequest;
import br.com.fiap.challange.oficina.dto.request.VeiculoRequest;
import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VeiculoControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    VeiculoRepositoryPort veiculoRepository;
    @Autowired
    ClienteRepositoryPort clienteRepository;
    @Autowired
    UsuarioRepositoryPort usuarioRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private String token;
    private Long clienteId;

    @BeforeEach
    void setUp() throws Exception {
        veiculoRepository.deleteAll();
        clienteRepository.deleteAll();

        if (!usuarioRepository.existsByUsername("admin_veiculo_it")) {
            usuarioRepository.save(Usuario.builder()
                    .username("admin_veiculo_it").password(passwordEncoder.encode("admin123"))
                    .role("ADMIN").ativo(true).build());
        }
        UserDetails userDetails = new User("admin_veiculo_it", "admin123",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token = "Bearer " + jwtService.generateToken(userDetails);

        String clienteJson = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClienteRequest("Dono do Carro", "529.982.247-25", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        clienteId = objectMapper.readTree(clienteJson).get("id").asLong();
    }

    @AfterEach
    void tearDown() {
        veiculoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void deveCadastrarVeiculoComSucesso() throws Exception {
        VeiculoRequest request = new VeiculoRequest("ABC1234", "Toyota", "Corolla", 2020, clienteId);

        mockMvc.perform(post("/api/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1234"))
                .andExpect(jsonPath("$.marca").value("Toyota"));
    }

    @Test
    void deveListarVeiculos() throws Exception {
        mockMvc.perform(get("/api/veiculos")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveBuscarVeiculoPorPlaca() throws Exception {
        mockMvc.perform(post("/api/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VeiculoRequest("MRC1E23", "Honda", "Civic", 2022, clienteId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/veiculos/placa/MRC1E23")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("MRC1E23"));
    }

    @Test
    void deveBuscarVeiculosPorCliente() throws Exception {
        mockMvc.perform(post("/api/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VeiculoRequest("DEF5678", "Ford", "Ka", 2018, clienteId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/veiculos/cliente/" + clienteId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveAtualizarVeiculo() throws Exception {
        String json = mockMvc.perform(post("/api/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VeiculoRequest("GHI9012", "Fiat", "Uno", 2015, clienteId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(put("/api/veiculos/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VeiculoRequest("GHI9012", "Fiat", "Palio", 2016, clienteId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Palio"));
    }

    @Test
    void deveDeletarVeiculo() throws Exception {
        String json = mockMvc.perform(post("/api/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VeiculoRequest("JKL3456", "VW", "Gol", 2019, clienteId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(delete("/api/veiculos/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/veiculos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404QuandoVeiculoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/api/veiculos/placa/ZZZ9999")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ParaPlacaInvalida() throws Exception {
        mockMvc.perform(post("/api/veiculos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VeiculoRequest("INVALIDA", "Toyota", "Corolla", 2020, clienteId))))
                .andExpect(status().isBadRequest());
    }
}
