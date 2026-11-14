package br.com.fiap.challange.oficina.bdd;

import br.com.fiap.challange.oficina.domain.port.in.OrquestradorSagaInputPort;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.mensageria.RelayOutbox;
import br.com.fiap.challange.oficina.infrastructure.messaging.CanalEmMemoria;
import br.com.fiap.challange.oficina.infrastructure.messaging.Mensagem;
import br.com.fiap.challange.oficina.infrastructure.messaging.ReceptorMensagens;
import br.com.fiap.challange.oficina.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os passos falam com o OS Service de verdade (REST + banco + outbox) e
 * fazem o papel do Billing e do Execution Service: conferem o comando que
 * saiu na outbox e respondem com o evento correspondente, do mesmo jeito que
 * chegaria pelo Pub/Sub.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SagaSteps {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JwtService jwtService;
    @Autowired
    ReceptorMensagens receptor;
    @Autowired
    RelayOutbox relay;
    @Autowired
    CanalEmMemoria canal;
    @Autowired
    OrquestradorSagaInputPort orquestrador;
    @Autowired
    PecaRepositoryPort pecas;

    private String token;
    private Long clienteId;
    private String placa;
    private Long osId;
    private Long pecaId;
    private Mensagem ultimaMensagem;

    @Before
    public void limparBanco() {
        for (String tabela : List.of("saga_passos", "sagas_ordem_servico", "historico_status_os", "outbox_mensagens",
                "mensagens_processadas", "itens_peca_os", "itens_servico_os", "ordens_servico", "veiculos",
                "clientes", "pecas", "servicos")) {
            jdbc.execute("DELETE FROM " + tabela);
        }
        canal.limpar();
        token = "Bearer " + jwtService.generateToken("admin", "ADMIN");
    }

    // ─────────────── preparação ───────────────

    @Dado("um cliente com o veículo {string} cadastrado")
    public void clienteComVeiculo(String placa) throws Exception {
        this.placa = placa;
        clienteId = idDe(enviar("/api/clientes", Map.of("nome", "Carla Mendes", "documento", "529.982.247-25",
                "email", "carla@exemplo.com")));
        enviar("/api/veiculos", Map.of("placa", placa, "marca", "Fiat", "modelo", "Argo", "ano", 2022,
                "clienteId", clienteId));
    }

    @E("uma OS aberta com o serviço {string} de R$ {string} e {int} peças {string} de R$ {string} com {int} em estoque")
    public void osAberta(String servico, String precoServico, int qtdPeca, String peca, String precoPeca, int estoque)
            throws Exception {
        Long servicoId = idDe(enviar("/api/servicos", Map.of("nome", servico, "preco", valor(precoServico),
                "tempoEstimadoMinutos", 90)));
        pecaId = idDe(enviar("/api/pecas", Map.of("nome", peca, "precoUnitario", valor(precoPeca),
                "quantidadeEstoque", estoque)));
        osId = idDe(enviar("/api/ordens-servico", Map.of("documentoCliente", "52998224725", "placaVeiculo", placa,
                "descricaoProblema", "Barulho ao frear",
                "servicos", List.of(Map.of("servicoId", servicoId, "quantidade", 1)),
                "pecas", List.of(Map.of("pecaId", pecaId, "quantidade", qtdPeca)))));
    }

    @E("o mecânico iniciou o diagnóstico")
    public void diagnostico() throws Exception {
        mvc.perform(post("/api/ordens-servico/{id}/iniciar-diagnostico", osId).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Dado("que o orçamento foi gerado")
    public void orcamentoGerado() throws Exception {
        pedirOrcamento();
        billingResponde("ORCAMENTO_GERADO");
    }

    @Dado("que o cliente já aprovou o orçamento")
    public void orcamentoAprovado() throws Exception {
        orcamentoGerado();
        billingResponde("ORCAMENTO_APROVADO");
    }

    @Dado("que o pagamento já foi confirmado")
    public void pagamentoConfirmado() throws Exception {
        orcamentoAprovado();
        billingResponde("PAGAMENTO_APROVADO");
    }

    @E("o estoque da peça zerou")
    public void estoqueZerou() {
        var peca = pecas.findById(pecaId).orElseThrow();
        peca.setQuantidadeEstoque(0);
        pecas.save(peca);
    }

    @E("o prazo da saga venceu")
    public void prazoVenceu() {
        jdbc.update("UPDATE sagas_ordem_servico SET prazo = DATEADD('HOUR', -1, CURRENT_TIMESTAMP) WHERE ordem_servico_id = ?",
                osId);
    }

    // ─────────────── ações ───────────────

    @Quando("o mecânico pede o orçamento")
    public void pedirOrcamento() throws Exception {
        mvc.perform(post("/api/ordens-servico/{id}/gerar-orcamento", osId).header("Authorization", token))
                .andExpect(status().isAccepted());
    }

    @Quando("o Billing responde {string}")
    public void billingResponde(String tipo) {
        responder(tipo);
    }

    @Quando("o Execution Service responde {string}")
    public void execucaoResponde(String tipo) {
        responder(tipo);
    }

    @Quando("o Billing reenvia a última mensagem")
    public void reenviar() {
        receptor.receber(ultimaMensagem);
    }

    @Quando("o verificador de prazos roda")
    public void verificarPrazos() {
        assertThat(orquestrador.verificarPrazos()).isEqualTo(1);
    }

    @Quando("o atendente registra a entrega do veículo")
    public void entregar() throws Exception {
        mvc.perform(post("/api/ordens-servico/{id}/entregar", osId).header("Authorization", token))
                .andExpect(status().isOk());
    }

    // ─────────────── verificações ───────────────

    @Entao("a OS fica {string}")
    public void osFica(String status) throws Exception {
        assertThat(json(get("/api/ordens-servico/{id}", osId)).path("status").asText()).isEqualTo(status);
    }

    @E("o comando {string} é enviado para o Billing com valor total de R$ {string}")
    public void comandoComValor(String tipo, String total) {
        Mensagem comando = comandoEnviado(tipo, "billing-comandos");
        assertThat(comando.dados().path("valorTotal").decimalValue()).isEqualByComparingTo(valor(total));
        assertThat(comando.dados().path("cliente").path("email").asText()).isEqualTo("carla@exemplo.com");
    }

    @E("o comando {string} é enviado para o Billing")
    public void comandoBilling(String tipo) {
        comandoEnviado(tipo, "billing-comandos");
    }

    @E("o comando {string} é enviado para o Execution Service")
    public void comandoExecucao(String tipo) {
        Mensagem comando = comandoEnviado(tipo, "execucao-comandos");
        assertThat(comando.dados().path("itens")).hasSize(2);
    }

    @E("o estoque da peça fica em {int}")
    public void estoque(int quantidade) {
        assertThat(pecas.findById(pecaId).orElseThrow().getQuantidadeEstoque()).isEqualTo(quantidade);
    }

    @E("a saga fica na etapa {string}")
    public void etapaDaSaga(String etapa) throws Exception {
        assertThat(saga().path("etapa").asText()).isEqualTo(etapa);
    }

    @E("o histórico da OS mostra {string} uma única vez")
    public void historicoUnico(String status) throws Exception {
        JsonNode historico = json(get("/api/ordens-servico/{id}/historico", osId));
        long vezes = 0;
        for (JsonNode linha : historico) {
            if (status.equals(linha.path("statusNovo").asText())) {
                vezes++;
            }
        }
        assertThat(vezes).isEqualTo(1);
    }

    // ─────────────── apoio ───────────────

    private void responder(String tipo) {
        Map<String, Object> dados = switch (tipo) {
            case "ORCAMENTO_GERADO" -> Map.of("orcamentoId", "orc-bdd", "valorTotal", 360);
            case "ORCAMENTO_APROVADO" -> Map.of("orcamentoId", "orc-bdd", "linkPagamento", "https://mp.exemplo/checkout");
            case "ORCAMENTO_REJEITADO" -> Map.of("orcamentoId", "orc-bdd", "motivo", "Achei caro");
            case "PAGAMENTO_APROVADO" -> Map.of("pagamentoId", "pay-bdd", "valor", 360);
            case "PAGAMENTO_RECUSADO" -> Map.of("pagamentoId", "pay-bdd", "motivo", "cc_rejected_insufficient_amount");
            case "EXECUCAO_ATUALIZADA" -> Map.of("statusExecucao", "EM_DIAGNOSTICO", "responsavel", "joao.mecanico");
            case "EXECUCAO_FINALIZADA" -> Map.of("statusExecucao", "FINALIZADA", "observacoes", "Pastilhas trocadas");
            case "EXECUCAO_FALHOU" -> Map.of("statusExecucao", "FALHOU", "motivo", "Disco de freio empenado");
            default -> Map.of();
        };
        ultimaMensagem = Mensagem.nova(tipo, sagaIdAtual(), osId, mapper.valueToTree(dados));
        receptor.receber(ultimaMensagem);
    }

    private Mensagem comandoEnviado(String tipo, String destino) {
        relay.publicarPendentes();
        List<CanalEmMemoria.Enviada> enviadas = canal.enviadas().stream()
                .filter(e -> e.mensagem().tipo().equals(tipo) && e.mensagem().ordemServicoId().equals(osId))
                .toList();
        assertThat(enviadas).as("comando %s publicado", tipo).isNotEmpty();
        CanalEmMemoria.Enviada enviada = enviadas.getLast();
        assertThat(enviada.destino()).isEqualTo(destino);
        assertThat(enviada.mensagem().sagaId()).isEqualTo(sagaIdAtual());
        return enviada.mensagem();
    }

    private String sagaIdAtual() {
        try {
            return saga().path("id").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode saga() throws Exception {
        return json(get("/api/ordens-servico/{id}/saga", osId)).get(0);
    }

    private JsonNode json(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        if (request instanceof org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
            builder.header("Authorization", token);
        }
        return mapper.readTree(mvc.perform(request).andReturn().getResponse().getContentAsString());
    }

    private ResultActions enviar(String url, Map<String, Object> corpo) throws Exception {
        return mvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(corpo))).andExpect(status().isCreated());
    }

    private Long idDe(ResultActions resultado) throws Exception {
        return mapper.readTree(resultado.andReturn().getResponse().getContentAsString()).path("id").asLong();
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto.replace(".", "").replace(",", "."));
    }
}
