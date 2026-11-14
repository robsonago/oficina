package br.com.fiap.challange.oficina.saga;

import br.com.fiap.challange.oficina.application.usecase.EstoqueService;
import br.com.fiap.challange.oficina.application.usecase.GestorStatusOS;
import br.com.fiap.challange.oficina.application.usecase.OrquestradorSagaOS;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.model.ItemServicoOS;
import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.SagaOrdemServico;
import br.com.fiap.challange.oficina.domain.model.Servico;
import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.command.EventoSagaCommand;
import br.com.fiap.challange.oficina.domain.port.out.ComandosSagaPort;
import br.com.fiap.challange.oficina.domain.port.out.HistoricoStatusRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.MetricasSagaPort;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.SagaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Casos de borda do orquestrador que o BDD não percorre: eventos fora de
 * ordem, saga desconhecida, falha do Billing e prazos de cada etapa.
 */
@ExtendWith(MockitoExtension.class)
class OrquestradorSagaOSTest {

    @Mock
    SagaRepositoryPort sagas;
    @Mock
    OrdemServicoRepositoryPort ordens;
    @Mock
    ComandosSagaPort comandos;
    @Mock
    HistoricoStatusRepositoryPort historico;
    @Mock
    PecaRepositoryPort pecas;
    @Mock
    MetricasSagaPort metricas;

    OrquestradorSagaOS orquestrador;

    @BeforeEach
    void setUp() {
        orquestrador = new OrquestradorSagaOS(sagas, ordens, comandos, new GestorStatusOS(ordens, historico),
                new EstoqueService(pecas), metricas, Duration.ofMinutes(5), Duration.ofHours(72), Duration.ofHours(48));
        lenient().when(ordens.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sagas.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private OrdemServico os(StatusOS status, boolean comItens) {
        OrdemServico os = OrdemServico.builder().id(1L).numero("OS-1").status(status)
                .cliente(Cliente.builder().id(1L).nome("Ana").documento("52998224725").email("ana@x.com").build())
                .itensServico(new ArrayList<>()).itensPeca(new ArrayList<>()).build();
        if (comItens) {
            os.getItensServico().add(ItemServicoOS.builder().ordemServico(os)
                    .servico(Servico.builder().id(3L).nome("Alinhamento").preco(new BigDecimal("80")).build())
                    .quantidade(1).precoUnitario(new BigDecimal("80")).build());
        }
        when(ordens.findById(1L)).thenReturn(Optional.of(os));
        return os;
    }

    private SagaOrdemServico saga(EtapaSaga etapa) {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(1L, LocalDateTime.now().plusMinutes(5));
        saga.setEtapa(etapa);
        lenient().when(sagas.buscarPorId(saga.getId())).thenReturn(Optional.of(saga));
        return saga;
    }

    private void evento(String tipo, SagaOrdemServico saga, Map<String, Object> dados) {
        orquestrador.tratar(new EventoSagaCommand(tipo, saga.getId(), 1L, dados));
    }

    @Test
    void iniciarMandaGerarOrcamentoComOsItens() {
        os(StatusOS.EM_DIAGNOSTICO, true);
        when(sagas.buscarAtivaPorOrdemServico(1L)).thenReturn(Optional.empty());

        SagaOrdemServico saga = orquestrador.iniciar(1L, "mecanico");

        assertThat(saga.getEtapa()).isEqualTo(EtapaSaga.GERANDO_ORCAMENTO);
        verify(comandos).enviar(eq(ComandosSagaPort.BILLING), eq("GERAR_ORCAMENTO"), eq(saga.getId()), eq(1L),
                argThat(d -> ((List<?>) d.get("itens")).size() == 1 && d.get("numeroOS").equals("OS-1")));
        verify(metricas).sagaIniciada();
    }

    @Test
    void naoIniciaDuasSagasParaAMesmaOS() {
        os(StatusOS.EM_DIAGNOSTICO, true);
        SagaOrdemServico ativa = saga(EtapaSaga.AGUARDANDO_PAGAMENTO);
        when(sagas.buscarAtivaPorOrdemServico(1L)).thenReturn(Optional.of(ativa));

        assertThatThrownBy(() -> orquestrador.iniciar(1L, "mecanico"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe uma saga");
        verifyNoInteractions(comandos);
    }

    @Test
    void naoIniciaForaDoDiagnostico() {
        os(StatusOS.RECEBIDA, true);
        assertThatThrownBy(() -> orquestrador.iniciar(1L, "x")).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void osInexistente() {
        when(ordens.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orquestrador.iniciar(9L, "x")).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void eventoDeSagaDesconhecidaEIgnorado() {
        when(sagas.buscarPorId("nao-existe")).thenReturn(Optional.empty());

        orquestrador.tratar(new EventoSagaCommand("ORCAMENTO_GERADO", "nao-existe", 1L, Map.of()));

        verify(sagas, never()).salvar(any());
        verifyNoInteractions(ordens);
    }

    @Test
    void eventoForaDeOrdemFicaRegistradoMasNaoMudaNada() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, true);
        SagaOrdemServico saga = saga(EtapaSaga.GERANDO_ORCAMENTO);

        evento("PAGAMENTO_APROVADO", saga, Map.of("pagamentoId", "p"));
        evento("TIPO_QUE_NAO_EXISTE", saga, Map.of());

        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_ORCAMENTO);
        assertThat(saga.getEtapa()).isEqualTo(EtapaSaga.GERANDO_ORCAMENTO);
        assertThat(saga.getPassos()).extracting(p -> p.descricao()).anyMatch(d -> d.startsWith("Ignorado"));
        verifyNoInteractions(comandos);
    }

    @Test
    void billingNaoConseguiuGerarOOrcamento() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, true);
        SagaOrdemServico saga = saga(EtapaSaga.GERANDO_ORCAMENTO);

        evento("ORCAMENTO_FALHOU", saga, Map.of("motivo", "sem itens"));

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(saga.getEtapa()).isEqualTo(EtapaSaga.COMPENSADA);
        verify(metricas).sagaCompensada("orcamento_falhou");
    }

    @Test
    void confirmacaoDeCompensacaoForaDeHoraEIgnorada() {
        os(StatusOS.FINALIZADA, true);
        SagaOrdemServico saga = saga(EtapaSaga.CONCLUIDA);

        evento("PAGAMENTO_ESTORNADO", saga, Map.of());

        assertThat(saga.getEtapa()).isEqualTo(EtapaSaga.CONCLUIDA);
    }

    @Test
    void prazoDoBillingVencidoVoltaParaDiagnostico() {
        OrdemServico os = os(StatusOS.AGUARDANDO_ORCAMENTO, true);
        SagaOrdemServico saga = saga(EtapaSaga.GERANDO_ORCAMENTO);
        when(sagas.buscarComPrazoVencido(any())).thenReturn(List.of(saga));

        assertThat(orquestrador.verificarPrazos()).isEqualTo(1);

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(saga.getEtapa()).isEqualTo(EtapaSaga.COMPENSANDO);
        verify(comandos).enviar(eq(ComandosSagaPort.BILLING), eq("CANCELAR_ORCAMENTO"), eq(saga.getId()), eq(1L), anyMap());
    }

    @Test
    void clienteNaoRespondeuAoOrcamentoNoPrazo() {
        OrdemServico os = os(StatusOS.AGUARDANDO_APROVACAO, true);
        SagaOrdemServico saga = saga(EtapaSaga.AGUARDANDO_APROVACAO);
        when(sagas.buscarComPrazoVencido(any())).thenReturn(List.of(saga));

        orquestrador.verificarPrazos();

        assertThat(os.getStatus()).isEqualTo(StatusOS.CANCELADA);
        verify(metricas).sagaCompensada("prazo");
    }

    @Test
    void etapaSemPrazoNaoECompensada() {
        os(StatusOS.EM_EXECUCAO, true);
        SagaOrdemServico saga = saga(EtapaSaga.EM_EXECUCAO);
        when(sagas.buscarComPrazoVencido(any())).thenReturn(List.of(saga));

        orquestrador.verificarPrazos();

        assertThat(saga.getEtapa()).isEqualTo(EtapaSaga.EM_EXECUCAO);
        verifyNoInteractions(comandos);
    }

    @Test
    void finalizacaoQueChegaAntesDoInicioPassaPorEmExecucao() {
        OrdemServico os = os(StatusOS.AGUARDANDO_EXECUCAO, true);
        SagaOrdemServico saga = saga(EtapaSaga.AGUARDANDO_EXECUCAO);

        evento("EXECUCAO_FINALIZADA", saga, Map.of());

        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        assertThat(os.getDataInicio()).isNotNull();
        verify(historico, times(2)).registrar(any());
        verify(metricas).sagaConcluida();
    }

    @Test
    void andamentoDaExecucaoViraAnotacaoNoHistorico() {
        OrdemServico os = os(StatusOS.EM_EXECUCAO, true);
        SagaOrdemServico saga = saga(EtapaSaga.EM_EXECUCAO);

        evento("EXECUCAO_ATUALIZADA", saga, Map.of("statusExecucao", "EM_REPARO"));

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        verify(historico).registrar(argThat(h -> h.statusAnterior() == h.statusNovo()
                && h.observacao().equals("Execução: EM_REPARO")));
    }

    @Test
    void consultaDasSagasDaOS() {
        SagaOrdemServico concluida = saga(EtapaSaga.CONCLUIDA);
        when(sagas.buscarPorOrdemServico(1L)).thenReturn(List.of(concluida));
        assertThat(orquestrador.sagasDaOrdemServico(1L)).hasSize(1);
    }
}
