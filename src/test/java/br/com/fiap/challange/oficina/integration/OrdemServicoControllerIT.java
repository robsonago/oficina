package br.com.fiap.challange.oficina.integration;

import br.com.fiap.challange.oficina.dto.request.*;
import br.com.fiap.challange.oficina.domain.model.Usuario;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.ServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.UsuarioRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.ItemServicoOSJpaRepository;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.ItemPecaOSJpaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdemServicoControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    OrdemServicoRepositoryPort osRepository;
    @Autowired
    ItemServicoOSJpaRepository itemServicoOSRepository;
    @Autowired
    ItemPecaOSJpaRepository itemPecaOSRepository;
    @Autowired
    VeiculoRepositoryPort veiculoRepository;
    @Autowired
    ClienteRepositoryPort clienteRepository;
    @Autowired
    UsuarioRepositoryPort usuarioRepository;
    @Autowired
    PecaRepositoryPort pecaRepository;
    @Autowired
    ServicoRepositoryPort servicoRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired
    br.com.fiap.challange.oficina.infrastructure.messaging.ReceptorMensagens receptor;

    private String token;
    private Long osId;

    private void limparTabelasDaSaga() {
        for (String tabela : List.of("saga_passos", "sagas_ordem_servico", "historico_status_os",
                "outbox_mensagens", "mensagens_processadas")) {
            jdbc.execute("DELETE FROM " + tabela);
        }
    }

    private Long criarServico() throws Exception {
        String json = mockMvc.perform(post("/api/servicos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Revisão\",\"descricao\":\"Revisão geral\",\"preco\":200.00,\"tempoEstimadoMinutos\":60}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private void prepararParaOrcamento() throws Exception {
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/iniciar-diagnostico").header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/servicos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicoId\":" + criarServico() + ",\"quantidade\":1}"))
                .andExpect(status().isOk());
    }

    private String sagaAtual() throws Exception {
        String json = mockMvc.perform(get("/api/ordens-servico/" + osId + "/saga").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get(0).get("id").asText();
    }

    private void evento(String tipo, String sagaId, java.util.Map<String, Object> dados) {
        receptor.receber(br.com.fiap.challange.oficina.infrastructure.messaging.Mensagem.nova(
                tipo, sagaId, osId, objectMapper.valueToTree(dados)));
    }

    @BeforeEach
    void setUp() throws Exception {
        limparTabelasDaSaga();
        osRepository.deleteAll();
        veiculoRepository.deleteAll();
        clienteRepository.deleteAll();

        if (!usuarioRepository.existsByUsername("tecnico_it")) {
            usuarioRepository.save(Usuario.builder()
                    .username("tecnico_it").password(passwordEncoder.encode("senha123"))
                    .role("TECNICO").ativo(true).build());
        }
        token = "Bearer " + jwtService.generateToken("tecnico_it", "TECNICO");

        String clienteResult = mockMvc.perform(post("/api/clientes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClienteRequest("Ana Costa", "529.982.247-25", null, null, null))))
                .andReturn().getResponse().getContentAsString();
        Long clienteId = objectMapper.readTree(clienteResult).get("id").asLong();

        mockMvc.perform(post("/api/veiculos")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new VeiculoRequest("ABC1234", "Honda", "Civic", 2021, clienteId)))).andReturn();

        OrdemServicoRequest osRequest = new OrdemServicoRequest(
                "52998224725", "ABC1234", "Verificação geral", null, null, null);

        String osResult = mockMvc.perform(post("/api/ordens-servico")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(osRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        osId = objectMapper.readTree(osResult).get("id").asLong();
    }

    @AfterEach
    void tearDown() {
        limparTabelasDaSaga();
        osRepository.deleteAll();
        pecaRepository.deleteAll();
        servicoRepository.deleteAll();
        veiculoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void deveCriarOrdemServicoComStatusRecebida() throws Exception {
        mockMvc.perform(get("/api/ordens-servico/" + osId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.numero").isNotEmpty())
                .andExpect(jsonPath("$.valorTotal").value(0));
    }

    @Test
    void deveConsultarStatusPublicamente() throws Exception {
        mockMvc.perform(get("/api/ordens-servico/" + osId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Recebida"));
    }

    @Test
    void deveExecutarFluxoCompletoPelaSaga() throws Exception {
        prepararParaOrcamento();

        mockMvc.perform(post("/api/ordens-servico/" + osId + "/gerar-orcamento").header("Authorization", token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_ORCAMENTO"));
        String sagaId = sagaAtual();

        evento("ORCAMENTO_GERADO", sagaId, java.util.Map.of("orcamentoId", "orc-1", "valorTotal", 200));
        evento("ORCAMENTO_APROVADO", sagaId, java.util.Map.of("orcamentoId", "orc-1", "linkPagamento", "http://pagar"));
        evento("PAGAMENTO_APROVADO", sagaId, java.util.Map.of("pagamentoId", "pay-1"));
        evento("EXECUCAO_ATUALIZADA", sagaId, java.util.Map.of("statusExecucao", "EM_REPARO"));
        evento("EXECUCAO_FINALIZADA", sagaId, java.util.Map.of());

        mockMvc.perform(post("/api/ordens-servico/" + osId + "/entregar").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        mockMvc.perform(get("/api/ordens-servico/" + osId + "/saga").header("Authorization", token))
                .andExpect(jsonPath("$[0].etapa").value("CONCLUIDA"))
                .andExpect(jsonPath("$[0].linkPagamento").value("http://pagar"));
        mockMvc.perform(get("/api/ordens-servico/" + osId + "/historico").header("Authorization", token))
                .andExpect(jsonPath("$[0].statusNovo").value("RECEBIDA"))
                .andExpect(jsonPath("$[-1:].statusNovo").value("ENTREGUE"));
    }

    @Test
    void devePermitirRejeitarOrcamentoEVoltarParaDiagnostico() throws Exception {
        prepararParaOrcamento();
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/gerar-orcamento").header("Authorization", token))
                .andExpect(status().isAccepted());
        String sagaId = sagaAtual();

        evento("ORCAMENTO_GERADO", sagaId, java.util.Map.of("orcamentoId", "orc-1"));
        evento("ORCAMENTO_REJEITADO", sagaId, java.util.Map.of("motivo", "Valor alto"));

        mockMvc.perform(get("/api/ordens-servico/" + osId).header("Authorization", token))
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
        mockMvc.perform(get("/api/ordens-servico/" + osId + "/saga").header("Authorization", token))
                .andExpect(jsonPath("$[0].etapa").value("COMPENSADA"));
    }

    @Test
    void naoGeraOrcamentoSemItensNemForaDoDiagnostico() throws Exception {
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/gerar-orcamento").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/iniciar-diagnostico").header("Authorization", token));
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/gerar-orcamento").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("ao menos um serviço")));
    }

    @Test
    void acompanhamentoFuncionaMesmoComOsOutrosServicosForaDoAr() throws Exception {
        mockMvc.perform(get("/api/ordens-servico/" + osId + "/acompanhamento").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordemServico.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.historico.length()").value(1))
                .andExpect(jsonPath("$.indisponiveis.length()").value(2));
    }

    @Test
    void deveRetornar422ParaTransicaoInvalida() throws Exception {
        mockMvc.perform(post("/api/ordens-servico/" + osId + "/entregar")
                        .header("Authorization", token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveListarOrdensPorStatus() throws Exception {
        mockMvc.perform(get("/api/ordens-servico/status/RECEBIDA")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveRetornarEstatisticas() throws Exception {
        mockMvc.perform(get("/api/ordens-servico/estatisticas")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoMedioExecucaoMinutos").exists())
                .andExpect(jsonPath("$.totalOSPorStatus_recebida").value(1));
    }

    @Test
    void deveListarTodasOrdensDeServico() throws Exception {
        mockMvc.perform(get("/api/ordens-servico")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveListarOSAtivas() throws Exception {
        mockMvc.perform(get("/api/ordens-servico/ativas")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("RECEBIDA"));
    }

    @Test
    void deveAdicionarServicoAOrdem() throws Exception {
        String servicoJson = mockMvc.perform(post("/api/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServicoRequest("Troca de óleo", null, new java.math.BigDecimal("150.00"), 60))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long servicoId = objectMapper.readTree(servicoJson).get("id").asLong();

        mockMvc.perform(post("/api/ordens-servico/" + osId + "/iniciar-diagnostico")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ordens-servico/" + osId + "/servicos")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemServicoRequest(servicoId, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itensServico.length()").value(1));
    }

    @Test
    void deveAdicionarPecaAOrdem() throws Exception {
        String pecaJson = mockMvc.perform(post("/api/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PecaRequest("Filtro", null, new java.math.BigDecimal("45.00"), 10, "FILT-001"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long pecaId = objectMapper.readTree(pecaJson).get("id").asLong();

        mockMvc.perform(post("/api/ordens-servico/" + osId + "/iniciar-diagnostico")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ordens-servico/" + osId + "/pecas")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemPecaRequest(pecaId, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itensPeca.length()").value(1));
    }
}
