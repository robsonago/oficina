package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.ItemPecaRequest;
import br.com.fiap.challange.oficina.dto.request.ItemServicoRequest;
import br.com.fiap.challange.oficina.dto.request.OrdemServicoRequest;
import br.com.fiap.challange.oficina.dto.response.EstatisticasResponse;
import br.com.fiap.challange.oficina.dto.response.OrdemServicoResponse;
import br.com.fiap.challange.oficina.exception.EstoqueInsuficienteException;
import br.com.fiap.challange.oficina.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.model.*;
import br.com.fiap.challange.oficina.model.enums.StatusOS;
import br.com.fiap.challange.oficina.repository.*;
import br.com.fiap.challange.oficina.validator.CpfCnpjValidator;
import br.com.fiap.challange.oficina.validator.PlacaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository osRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;

    public OrdemServicoResponse criar(OrdemServicoRequest request) {
        log.info("Criando OS cliente={} placa={}", request.documentoCliente(), request.placaVeiculo());
        String docNormalizado = CpfCnpjValidator.normalizar(request.documentoCliente());
        Cliente cliente = clienteRepository.findByDocumento(docNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com documento: " + request.documentoCliente()));

        String placaNormalizada = PlacaValidator.normalizar(request.placaVeiculo());
        Veiculo veiculo = veiculoRepository.findByPlaca(placaNormalizada)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com placa: " + request.placaVeiculo()));

        OrdemServico os = OrdemServico.builder()
                .numero(gerarNumero())
                .cliente(cliente)
                .veiculo(veiculo)
                .status(StatusOS.RECEBIDA)
                .descricaoProblema(request.descricaoProblema())
                .observacoes(request.observacoes())
                .dataAbertura(java.time.LocalDateTime.now())
                .build();

        if (request.servicos() != null) {
            for (ItemServicoRequest item : request.servicos()) {
                Servico servico = servicoRepository.findById(item.servicoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + item.servicoId()));
                ItemServicoOS itemOS = ItemServicoOS.builder()
                        .ordemServico(os).servico(servico)
                        .quantidade(item.quantidade()).precoUnitario(servico.getPreco()).build();
                os.getItensServico().add(itemOS);
            }
        }

        if (request.pecas() != null) {
            for (ItemPecaRequest item : request.pecas()) {
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
        OrdemServicoResponse response = OrdemServicoResponse.from(osRepository.save(os));
        log.info("OS criada numero={} id={} valorTotal={}", response.numero(), response.id(), response.valorTotal());
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar() {
        log.info("Listando OSs");
        return osRepository.findAll().stream().map(OrdemServicoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(Long id) {
        log.info("Buscando OS id={}", id);
        return OrdemServicoResponse.from(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listarPorStatus(StatusOS status) {
        log.info("Listando OSs status={}", status);
        return osRepository.findByStatus(status).stream().map(OrdemServicoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public String consultarStatus(Long id) {
        log.info("Consultando status OS id={}", id);
        return buscarEntidadePorId(id).getStatus().getDescricao();
    }

    public OrdemServicoResponse iniciarDiagnostico(Long id) {
        log.info("Iniciando diagnóstico OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.EM_DIAGNOSTICO);
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public OrdemServicoResponse gerarOrcamento(Long id) {
        log.info("Gerando orçamento OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.recalcularTotal();
        os.transicionarStatus(StatusOS.AGUARDANDO_APROVACAO);
        OrdemServicoResponse response = OrdemServicoResponse.from(osRepository.save(os));
        log.info("Orçamento gerado OS id={} valorTotal={}", id, response.valorTotal());
        return response;
    }

    public OrdemServicoResponse aprovarOrcamento(Long id) {
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
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public OrdemServicoResponse rejeitarOrcamento(Long id) {
        log.info("Rejeitando orçamento OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.EM_DIAGNOSTICO);
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public OrdemServicoResponse finalizar(Long id) {
        log.info("Finalizando OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.FINALIZADA);
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public OrdemServicoResponse entregar(Long id) {
        log.info("Registrando entrega OS id={}", id);
        OrdemServico os = buscarEntidadePorId(id);
        os.transicionarStatus(StatusOS.ENTREGUE);
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public OrdemServicoResponse adicionarServico(Long osId, ItemServicoRequest request) {
        log.info("Adicionando serviço OS id={} servicoId={}", osId, request.servicoId());
        OrdemServico os = buscarEntidadePorId(osId);
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + request.servicoId()));
        ItemServicoOS item = ItemServicoOS.builder()
                .ordemServico(os).servico(servico)
                .quantidade(request.quantidade()).precoUnitario(servico.getPreco()).build();
        os.getItensServico().add(item);
        os.recalcularTotal();
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public OrdemServicoResponse adicionarPeca(Long osId, ItemPecaRequest request) {
        log.info("Adicionando peça OS id={} pecaId={}", osId, request.pecaId());
        OrdemServico os = buscarEntidadePorId(osId);
        Peca peca = pecaRepository.findById(request.pecaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + request.pecaId()));
        validarEstoque(peca, request.quantidade());
        ItemPecaOS item = ItemPecaOS.builder()
                .ordemServico(os).peca(peca)
                .quantidade(request.quantidade()).precoUnitario(peca.getPrecoUnitario()).build();
        os.getItensPeca().add(item);
        os.recalcularTotal();
        return OrdemServicoResponse.from(osRepository.save(os));
    }

    public EstatisticasResponse calcularEstatisticas() {
        log.info("Calculando estatísticas de OSs");
        List<OrdemServico> finalizadas = osRepository.findFinalizadasComTempo();
        long tempoMedio = 0;
        if (!finalizadas.isEmpty()) {
            tempoMedio = (long) finalizadas.stream()
                    .mapToLong(os -> ChronoUnit.MINUTES.between(os.getDataInicio(), os.getDataFinalizacao()))
                    .average().orElse(0);
        }
        List<OrdemServico> todas = osRepository.findAll();
        return new EstatisticasResponse(
                tempoMedio, finalizadas.size(),
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

    private void validarEstoque(Peca peca, int quantidade) {
        if (peca.getQuantidadeEstoque() < quantidade) {
            throw new EstoqueInsuficienteException(
                    String.format("Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                            peca.getNome(), peca.getQuantidadeEstoque(), quantidade));
        }
    }

    private String gerarNumero() {
        return String.format("OS-%d-%s", java.time.Year.now().getValue(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private OrdemServico buscarEntidadePorId(Long id) {
        return osRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço não encontrada: " + id));
    }
}
