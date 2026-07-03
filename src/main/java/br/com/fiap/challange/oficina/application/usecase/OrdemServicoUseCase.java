package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.model.*;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.OrdemServicoInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.AbrirOrdemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemPecaCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AprovacaoOrcamentoCommand;
import br.com.fiap.challange.oficina.domain.port.out.*;
import br.com.fiap.challange.oficina.domain.validator.CpfCnpjValidator;
import br.com.fiap.challange.oficina.domain.validator.PlacaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrdemServicoUseCase implements OrdemServicoInputPort {

    private final OrdemServicoRepositoryPort osRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final VeiculoRepositoryPort veiculoRepository;
    private final ServicoRepositoryPort servicoRepository;
    private final PecaRepositoryPort pecaRepository;
    private final EmailPort emailPort;

    @Override
    public OrdemServico criar(AbrirOrdemServicoCommand command) {
        log.info("Criando OS cliente={} placa={}", command.documentoCliente(), command.placaVeiculo());
        String docNormalizado = CpfCnpjValidator.normalizar(command.documentoCliente());
        Cliente cliente = clienteRepository.findByDocumento(docNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com documento: " + command.documentoCliente()));

        String placaNormalizada = PlacaValidator.normalizar(command.placaVeiculo());
        Veiculo veiculo = veiculoRepository.findByPlaca(placaNormalizada)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com placa: " + command.placaVeiculo()));

        OrdemServico os = OrdemServico.builder()
                .numero(gerarNumero())
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .descricaoProblema(command.descricaoProblema())
                .observacoes(command.observacoes())
                .dataAbertura(LocalDateTime.now())
                .build();

        if (command.servicos() != null) {
            for (AdicionarItemServicoCommand item : command.servicos()) {
                Servico servico = servicoRepository.findById(item.servicoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + item.servicoId()));
                ItemServicoOS itemOS = ItemServicoOS.builder()
                        .ordemServico(os).servico(servico)
                        .quantidade(item.quantidade()).precoUnitario(servico.getPreco()).build();
                os.getItensServico().add(itemOS);
            }
        }

        if (command.pecas() != null) {
            for (AdicionarItemPecaCommand item : command.pecas()) {
                Peca peca = pecaRepository.findById(item.pecaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + item.pecaId()));
                validarEstoque(peca, item.quantidade());
                ItemPecaOS itemOS = ItemPecaOS.builder()
                        .ordemServico(os).peca(peca)
                        .quantidade(item.quantidade()).precoUnitario(peca.getPrecoUnitario()).build();
                os.getItensPeca().add(itemOS);
            }
        }

        os.recalcularTotal();
        OrdemServico salva = osRepository.save(os);
        log.info("OS criada numero={} id={} valorTotal={}", salva.getNumero(), salva.getId(), salva.getValorTotal());
        return salva;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServico> listar() {
        log.info("Listando OSs ativas por prioridade");
        return listarAtivas();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServico> listarAtivas() {
        log.info("Listando OSs ativas ordenadas por prioridade");
        List<StatusOS> prioridade = List.of(
                StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_APROVACAO,
                StatusOS.EM_DIAGNOSTICO, StatusOS.RECEBIDA);
        return osRepository.findByStatusIn(prioridade)
                .stream()
                .sorted(Comparator
                        .<OrdemServico, Integer>comparing(os -> prioridade.indexOf(os.getStatus()))
                        .thenComparing(OrdemServico::getDataAbertura))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemServico buscarPorId(Long id) {
        log.info("Buscando OS id={}", id);
        return buscarEntidadePorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServico> listarPorStatus(StatusOS status) {
        log.info("Listando OSs status={}", status);
        return osRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public String consultarStatus(Long id) {
        log.info("Consultando status OS id={}", id);
        return buscarEntidadePorId(id).getStatus().getDescricao();
    }

    @Override
    public OrdemServico iniciarDiagnostico(Long id) {
        log.info("Iniciando diagnóstico OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.EM_DIAGNOSTICO);
        return osRepository.save(os);
    }

    @Override
    public OrdemServico gerarOrcamento(Long id) {
        log.info("Gerando orçamento OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.recalcularTotal();
        os.transicionarStatus(StatusOS.AGUARDANDO_APROVACAO);
        OrdemServico salva = osRepository.save(os);
        log.info("Orçamento gerado OS id={} valorTotal={}", id, salva.getValorTotal());
        emailPort.enviarNotificacaoOrcamento(
                os.getCliente().getEmail(),
                os.getCliente().getNome(),
                os.getNumero(),
                os.getValorTotal());
        return salva;
    }

    @Override
    public OrdemServico aprovarOrcamento(Long id) {
        log.info("Aprovando orçamento OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        for (ItemPecaOS item : os.getItensPeca()) {
            validarEstoque(item.getPeca(), item.getQuantidade());
        }
        os.transicionarStatus(StatusOS.EM_EXECUCAO);
        for (ItemPecaOS item : os.getItensPeca()) {
            Peca peca = item.getPeca();
            peca.setQuantidadeEstoque(peca.getQuantidadeEstoque() - item.getQuantidade());
            pecaRepository.save(peca);
            log.info("Estoque baixado peça id={} nome={} qtd={}", peca.getId(), peca.getNome(), item.getQuantidade());
        }
        return osRepository.save(os);
    }

    @Override
    public OrdemServico rejeitarOrcamento(Long id) {
        log.info("Rejeitando orçamento OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.EM_DIAGNOSTICO);
        return osRepository.save(os);
    }

    @Override
    public OrdemServico aprovarOuRejeitarOrcamento(Long id, AprovacaoOrcamentoCommand command) {
        if (Boolean.TRUE.equals(command.aprovado())) {
            log.info("Aprovando orçamento via endpoint unificado OS id={}", id);
            return aprovarOrcamento(id);
        } else {
            log.info("Rejeitando orçamento via endpoint unificado OS id={} observacao={}", id, command.observacao());
            return rejeitarOrcamento(id);
        }
    }

    @Override
    public OrdemServico finalizar(Long id) {
        log.info("Finalizando OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.FINALIZADA);
        return osRepository.save(os);
    }

    @Override
    public OrdemServico entregar(Long id) {
        log.info("Registrando entrega OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.ENTREGUE);
        return osRepository.save(os);
    }

    @Override
    public OrdemServico adicionarServico(Long osId, AdicionarItemServicoCommand command) {
        log.info("Adicionando serviço OS id={} servicoId={}", osId, command.servicoId());
        OrdemServico os = buscarEntidadePorId(osId);
        validarStatusParaEdicaoDeItens(os);
        Servico servico = servicoRepository.findById(command.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + command.servicoId()));
        ItemServicoOS item = ItemServicoOS.builder()
                .ordemServico(os).servico(servico)
                .quantidade(command.quantidade()).precoUnitario(servico.getPreco()).build();
        os.getItensServico().add(item);
        os.recalcularTotal();
        return osRepository.save(os);
    }

    @Override
    public OrdemServico adicionarPeca(Long osId, AdicionarItemPecaCommand command) {
        log.info("Adicionando peça OS id={} pecaId={}", osId, command.pecaId());
        OrdemServico os = buscarEntidadePorId(osId);
        validarStatusParaEdicaoDeItens(os);
        Peca peca = pecaRepository.findById(command.pecaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + command.pecaId()));
        validarEstoque(peca, command.quantidade());
        ItemPecaOS item = ItemPecaOS.builder()
                .ordemServico(os).peca(peca)
                .quantidade(command.quantidade()).precoUnitario(peca.getPrecoUnitario()).build();
        os.getItensPeca().add(item);
        os.recalcularTotal();
        return osRepository.save(os);
    }

    @Override
    public Estatisticas calcularEstatisticas() {
        log.info("Calculando estatísticas de OSs");
        List<OrdemServico> finalizadas = osRepository.findFinalizadasComTempo();
        long tempoMedio = 0;
        if (!finalizadas.isEmpty()) {
            tempoMedio = (long) finalizadas.stream()
                    .mapToLong(os -> ChronoUnit.MINUTES.between(os.getDataInicio(), os.getDataFinalizacao()))
                    .average().orElse(0);
        }
        List<OrdemServico> todas = osRepository.findAll();
        return new Estatisticas(
                tempoMedio,
                finalizadas.size(),
                contarPorStatus(todas, StatusOS.RECEBIDA),
                contarPorStatus(todas, StatusOS.EM_DIAGNOSTICO),
                contarPorStatus(todas, StatusOS.AGUARDANDO_APROVACAO),
                contarPorStatus(todas, StatusOS.EM_EXECUCAO),
                contarPorStatus(todas, StatusOS.FINALIZADA),
                contarPorStatus(todas, StatusOS.ENTREGUE));
    }

    private long contarPorStatus(List<OrdemServico> lista, StatusOS status) {
        return lista.stream().filter(os -> os.getStatus() == status).count();
    }

    private void validarStatusParaEdicaoDeItens(OrdemServico os) {
        if (os.getStatus() != StatusOS.RECEBIDA && os.getStatus() != StatusOS.EM_DIAGNOSTICO) {
            throw new RegraDeNegocioException(
                    String.format("Itens só podem ser adicionados em OS com status RECEBIDA ou EM_DIAGNOSTICO. Status atual: %s",
                            os.getStatus().getDescricao()));
        }
    }

    private void validarEstoque(Peca peca, int quantidade) {
        if (peca.getQuantidadeEstoque() < quantidade) {
            throw new EstoqueInsuficienteException(
                    String.format("Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                            peca.getNome(), peca.getQuantidadeEstoque(), quantidade));
        }
    }

    private String gerarNumero() {
        String uuidSemHifens = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return String.format("OS-%d-%s", Year.now().getValue(), uuidSemHifens);
    }

    private OrdemServico buscarEntidadePorId(Long id) {
        return osRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço não encontrada: " + id));
    }
}
