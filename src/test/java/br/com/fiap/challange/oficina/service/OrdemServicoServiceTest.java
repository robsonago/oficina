package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.TransicaoStatusInvalidaException;
import br.com.fiap.challange.oficina.domain.model.*;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.model.enums.TipoDocumento;
import br.com.fiap.challange.oficina.domain.port.in.command.AbrirOrdemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemPecaCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AprovacaoOrcamentoCommand;
import br.com.fiap.challange.oficina.application.usecase.OrdemServicoUseCase;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.ServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.EmailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepositoryPort osRepository;
    @Mock
    private ClienteRepositoryPort clienteRepository;
    @Mock
    private VeiculoRepositoryPort veiculoRepository;
    @Mock
    private ServicoRepositoryPort servicoRepository;
    @Mock
    private PecaRepositoryPort pecaRepository;
    @Mock
    private EmailPort emailPort;

    @InjectMocks
    private OrdemServicoUseCase osService;

    @Test
    void deveCriarOrdemServicoComSucesso() {
        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                "52998224725", "ABC1234", "Motor falhando", null, null, null);

        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(clientePadrao()));
        when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.of(veiculoPadrao()));
        when(osRepository.save(any())).thenAnswer(inv -> {
            OrdemServico os = OrdemServico.builder()
                    .id(1L).numero("OS-2024-ABC12345").cliente(clientePadrao())
                    .veiculo(veiculoPadrao()).status(StatusOS.RECEBIDA)
                    .descricaoProblema("Motor falhando").dataAbertura(LocalDateTime.now())
                    .valorTotal(BigDecimal.ZERO).itensServico(new ArrayList<>()).itensPeca(new ArrayList<>())
                    .build();
            return os;
        });

        OrdemServico response = osService.criar(command);
        assertThat(response.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        assertThat(response.getNumero()).startsWith("OS-");
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                "52998224725", "ABC1234", null, null, null, null);
        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> osService.criar(command))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveLancarExcecaoQuandoVeiculoNaoEncontrado() {
        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                "52998224725", "ABC1234", null, null, null, null);
        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(clientePadrao()));
        when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> osService.criar(command))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveCriarOSComServicos() {
        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                "52998224725", "ABC1234", "Troca de óleo", null,
                List.of(new AdicionarItemServicoCommand(1L, 1)), null);

        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(clientePadrao()));
        when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.of(veiculoPadrao()));
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servicoPadrao()));
        when(osRepository.save(any())).thenAnswer(inv -> {
            OrdemServico os = inv.getArgument(0);
            if (os.getId() == null) {
                return OrdemServico.builder().id(1L).numero("OS-2024-TEST")
                        .cliente(clientePadrao()).veiculo(veiculoPadrao())
                        .status(StatusOS.RECEBIDA).dataAbertura(LocalDateTime.now())
                        .valorTotal(new BigDecimal("150.00"))
                        .itensServico(os.getItensServico()).itensPeca(new ArrayList<>()).build();
            }
            return os;
        });

        OrdemServico response = osService.criar(command);
        assertThat(response).isNotNull();
    }

    @Test
    void deveLancarExcecaoEstoqueInsuficiente() {
        Peca peca = pecaPadrao();
        peca.setQuantidadeEstoque(0);

        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                "52998224725", "ABC1234", null, null, null,
                List.of(new AdicionarItemPecaCommand(1L, 5)));

        when(clienteRepository.findByDocumento("52998224725")).thenReturn(Optional.of(clientePadrao()));
        when(veiculoRepository.findByPlaca("ABC1234")).thenReturn(Optional.of(veiculoPadrao()));
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> osService.criar(command))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void deveIniciarDiagnostico() {
        OrdemServico os = osPadrao(StatusOS.RECEBIDA);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.iniciarDiagnostico(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void deveGerarOrcamento() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.gerarOrcamento(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void deveAprovarOrcamento() {
        OrdemServico os = osPadrao(StatusOS.AGUARDANDO_APROVACAO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.aprovarOrcamento(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void deveRejeitarOrcamento() {
        OrdemServico os = osPadrao(StatusOS.AGUARDANDO_APROVACAO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.rejeitarOrcamento(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void deveFinalizar() {
        OrdemServico os = osPadrao(StatusOS.EM_EXECUCAO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.finalizar(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.FINALIZADA);
    }

    @Test
    void deveEntregar() {
        OrdemServico os = osPadrao(StatusOS.FINALIZADA);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.entregar(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.ENTREGUE);
    }

    @Test
    void deveLancarExcecaoTransicaoInvalida() {
        OrdemServico os = osPadrao(StatusOS.RECEBIDA);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> osService.finalizar(1L))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void deveCalcularEstatisticas() {
        when(osRepository.findFinalizadasComTempo()).thenReturn(List.of());
        when(osRepository.findAll()).thenReturn(List.of());

        Estatisticas stats = osService.calcularEstatisticas();
        assertThat(stats.tempoMedioExecucaoMinutos()).isEqualTo(0L);
    }

    @Test
    void deveLancarExcecaoQuandoOSNaoEncontrada() {
        when(osRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> osService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveBuscarOSPorIdComSucesso() {
        OrdemServico os = osPadrao(StatusOS.RECEBIDA);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));

        OrdemServico response = osService.buscarPorId(1L);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    void deveListarTodasAsOS() {
        when(osRepository.findAll()).thenReturn(List.of(osPadrao(StatusOS.RECEBIDA), osPadrao(StatusOS.EM_DIAGNOSTICO)));

        assertThat(osService.listar()).hasSize(2);
    }

    @Test
    void deveListarOSPorStatus() {
        when(osRepository.findByStatus(StatusOS.RECEBIDA)).thenReturn(List.of(osPadrao(StatusOS.RECEBIDA)));

        assertThat(osService.listarPorStatus(StatusOS.RECEBIDA)).hasSize(1);
    }

    @Test
    void deveConsultarStatusPublico() {
        OrdemServico os = osPadrao(StatusOS.EM_EXECUCAO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));

        String descricao = osService.consultarStatus(1L);
        assertThat(descricao).isEqualTo(StatusOS.EM_EXECUCAO.getDescricao());
    }

    @Test
    void deveAdicionarServicoNaOS() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servicoPadrao()));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.adicionarServico(1L, new AdicionarItemServicoCommand(1L, 2));
        assertThat(response).isNotNull();
        assertThat(os.getItensServico()).hasSize(1);
    }

    @Test
    void deveLancarExcecaoAoAdicionarServicoInexistente() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> osService.adicionarServico(1L, new AdicionarItemServicoCommand(99L, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAdicionarPecaNaOS() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(pecaPadrao()));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.adicionarPeca(1L, new AdicionarItemPecaCommand(1L, 2));
        assertThat(response).isNotNull();
        assertThat(os.getItensPeca()).hasSize(1);
    }

    @Test
    void deveLancarExcecaoAoAdicionarPecaInexistente() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(pecaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> osService.adicionarPeca(1L, new AdicionarItemPecaCommand(99L, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveLancarExcecaoAoAdicionarPecaSemEstoqueSuficiente() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        Peca peca = pecaPadrao();
        peca.setQuantidadeEstoque(1);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(pecaRepository.findById(1L)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> osService.adicionarPeca(1L, new AdicionarItemPecaCommand(1L, 5)))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void deveAprovarOrcamentoComDebitoDeEstoque() {
        Peca peca = pecaPadrao();
        ItemPecaOS itemPeca = ItemPecaOS.builder()
                .peca(peca).quantidade(3).precoUnitario(peca.getPrecoUnitario()).build();

        OrdemServico os = osPadrao(StatusOS.AGUARDANDO_APROVACAO);
        os.getItensPeca().add(itemPeca);

        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(pecaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.aprovarOrcamento(1L);
        assertThat(response.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(peca.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    void deveCalcularEstatisticasComOSFinalizadas() {
        OrdemServico osF = osPadrao(StatusOS.FINALIZADA);
        osF.setDataInicio(LocalDateTime.now().minusHours(2));
        osF.setDataFinalizacao(LocalDateTime.now());

        when(osRepository.findFinalizadasComTempo()).thenReturn(List.of(osF));
        when(osRepository.findAll()).thenReturn(List.of(osF));

        Estatisticas stats = osService.calcularEstatisticas();
        assertThat(stats.tempoMedioExecucaoMinutos()).isGreaterThan(0L);
        assertThat(stats.totalOSFinalizadas()).isEqualTo(1);
    }

    @Test
    void deveAprovarOrcamentoViaEndpointUnificado() {
        OrdemServico os = osPadrao(StatusOS.AGUARDANDO_APROVACAO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.aprovarOuRejeitarOrcamento(1L,
                new AprovacaoOrcamentoCommand(true, null));
        assertThat(response.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void deveRejeitarOrcamentoViaEndpointUnificado() {
        OrdemServico os = osPadrao(StatusOS.AGUARDANDO_APROVACAO);
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico response = osService.aprovarOuRejeitarOrcamento(1L,
                new AprovacaoOrcamentoCommand(false, "Valor alto"));
        assertThat(response.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void deveListarApenasOSAtivas() {
        List<StatusOS> ativos = List.of(StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_APROVACAO,
                StatusOS.EM_DIAGNOSTICO, StatusOS.RECEBIDA);
        OrdemServico osExecucao = osPadrao(StatusOS.EM_EXECUCAO);
        OrdemServico osRecebida = osPadrao(StatusOS.RECEBIDA);
        when(osRepository.findByStatusIn(ativos)).thenReturn(List.of(osRecebida, osExecucao));

        List<OrdemServico> result = osService.listarAtivas();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(result.get(1).getStatus()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    void deveEnviarEmailAoGerarOrcamento() {
        OrdemServico os = osPadrao(StatusOS.EM_DIAGNOSTICO);
        os.getCliente().setEmail("cliente@email.com");
        when(osRepository.findById(1L)).thenReturn(Optional.of(os));
        when(osRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        osService.gerarOrcamento(1L);

        org.mockito.Mockito.verify(emailPort).enviarNotificacaoOrcamento(
                org.mockito.ArgumentMatchers.eq("cliente@email.com"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    private OrdemServico osPadrao(StatusOS status) {
        return OrdemServico.builder()
                .id(1L).numero("OS-2024-TEST001")
                .cliente(clientePadrao()).veiculo(veiculoPadrao())
                .status(status).dataAbertura(LocalDateTime.now())
                .valorTotal(BigDecimal.ZERO)
                .itensServico(new ArrayList<>()).itensPeca(new ArrayList<>())
                .build();
    }

    private Cliente clientePadrao() {
        return Cliente.builder().id(1L).nome("João Silva")
                .documento("52998224725").tipoDocumento(TipoDocumento.CPF)
                .ativo(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Veiculo veiculoPadrao() {
        return Veiculo.builder().id(1L).placa("ABC1234").marca("Toyota")
                .modelo("Corolla").ano(2020).cliente(clientePadrao()).ativo(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Servico servicoPadrao() {
        return Servico.builder().id(1L).nome("Troca de óleo")
                .preco(new BigDecimal("150.00")).tempoEstimadoMinutos(60).ativo(true).build();
    }

    private Peca pecaPadrao() {
        return Peca.builder().id(1L).nome("Filtro de óleo")
                .precoUnitario(new BigDecimal("45.90")).quantidadeEstoque(10).ativo(true).build();
    }
}
