package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.EstoqueInsuficienteException;
import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.ItemPecaOS;
import br.com.fiap.challange.oficina.domain.model.ItemServicoOS;
import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.SagaOrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.OrquestradorSagaInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.EventoSagaCommand;
import br.com.fiap.challange.oficina.domain.port.out.ComandosSagaPort;
import br.com.fiap.challange.oficina.domain.port.out.MetricasSagaPort;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.SagaRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.com.fiap.challange.oficina.domain.model.TiposMensagem.*;

/**
 * Orquestrador da saga da OS (padrão Saga orquestrado).
 *
 * <pre>
 *  gerar-orcamento ──► GERAR_ORCAMENTO (Billing)
 *                      ◄── ORCAMENTO_GERADO ......... OS aguardando aprovação
 *  cliente aprova  ──► (Billing) ◄── ORCAMENTO_APROVADO
 *                      reserva peças no estoque ..... OS aguardando pagamento
 *  cliente paga    ──► (Mercado Pago → Billing) ◄── PAGAMENTO_APROVADO
 *                  ──► EXECUTAR_OS (Execution) ...... OS aguardando execução
 *                      ◄── EXECUCAO_ATUALIZADA ...... OS em execução
 *                      ◄── EXECUCAO_FINALIZADA ...... OS finalizada (saga concluída)
 * </pre>
 *
 * Compensações (desfazem na ordem inversa o que já tinha sido feito):
 * <ul>
 *   <li>orçamento falhou/rejeitado → OS volta para diagnóstico;</li>
 *   <li>sem estoque na aprovação → cancela o orçamento, OS cancelada;</li>
 *   <li>pagamento recusado ou prazo vencido → devolve as peças, cancela o orçamento, OS cancelada;</li>
 *   <li>falha na execução → devolve as peças, estorna o pagamento no Mercado Pago, OS cancelada.</li>
 * </ul>
 * Cada evento só é aceito na etapa em que faz sentido; fora dela é registrado
 * e ignorado, o que protege contra mensagens duplicadas ou fora de ordem.
 */
@Slf4j
@Service
@Transactional
public class OrquestradorSagaOS implements OrquestradorSagaInputPort {

    private static final String ORIGEM = "saga";

    private final SagaRepositoryPort sagas;
    private final OrdemServicoRepositoryPort ordens;
    private final ComandosSagaPort comandos;
    private final GestorStatusOS gestorStatus;
    private final EstoqueService estoque;
    private final MetricasSagaPort metricas;
    private final Duration prazoOrcamento;
    private final Duration prazoAprovacao;
    private final Duration prazoPagamento;

    public OrquestradorSagaOS(SagaRepositoryPort sagas, OrdemServicoRepositoryPort ordens, ComandosSagaPort comandos,
                              GestorStatusOS gestorStatus, EstoqueService estoque, MetricasSagaPort metricas,
                              @Value("${saga.prazo-orcamento:PT5M}") Duration prazoOrcamento,
                              @Value("${saga.prazo-aprovacao:PT72H}") Duration prazoAprovacao,
                              @Value("${saga.prazo-pagamento:PT48H}") Duration prazoPagamento) {
        this.sagas = sagas;
        this.ordens = ordens;
        this.comandos = comandos;
        this.gestorStatus = gestorStatus;
        this.estoque = estoque;
        this.metricas = metricas;
        this.prazoOrcamento = prazoOrcamento;
        this.prazoAprovacao = prazoAprovacao;
        this.prazoPagamento = prazoPagamento;
    }

    @Override
    public SagaOrdemServico iniciar(Long ordemServicoId, String solicitante) {
        OrdemServico os = carregarOS(ordemServicoId);
        if (os.getStatus() != StatusOS.EM_DIAGNOSTICO) {
            throw new RegraDeNegocioException("O orçamento só pode ser pedido com a OS em diagnóstico. Status atual: "
                    + os.getStatus().getDescricao());
        }
        if (os.getItensServico().isEmpty() && os.getItensPeca().isEmpty()) {
            throw new RegraDeNegocioException("Adicione ao menos um serviço ou peça antes de gerar o orçamento");
        }
        sagas.buscarAtivaPorOrdemServico(ordemServicoId).ifPresent(s -> {
            throw new RegraDeNegocioException("Já existe uma saga em andamento para esta OS: " + s.getId());
        });

        os.recalcularTotal();
        SagaOrdemServico saga = sagas.salvar(SagaOrdemServico.iniciar(ordemServicoId, agora().plus(prazoOrcamento)));
        gestorStatus.mudar(os, StatusOS.AGUARDANDO_ORCAMENTO, solicitante, "Orçamento solicitado ao Billing");
        comandos.enviar(ComandosSagaPort.BILLING, GERAR_ORCAMENTO, saga.getId(), os.getId(), dadosOrcamento(os));
        metricas.sagaIniciada();
        log.info("Saga {} iniciada para a OS {}", saga.getId(), os.getNumero());
        return saga;
    }

    @Override
    public void tratar(EventoSagaCommand evento) {
        SagaOrdemServico saga = sagas.buscarPorId(evento.sagaId()).orElse(null);
        if (saga == null) {
            log.warn("Evento {} para saga desconhecida {}, ignorado", evento.tipo(), evento.sagaId());
            return;
        }
        OrdemServico os = carregarOS(saga.getOrdemServicoId());
        boolean aplicado = switch (evento.tipo()) {
            case ORCAMENTO_GERADO -> orcamentoGerado(saga, os, evento);
            case ORCAMENTO_FALHOU -> orcamentoFalhou(saga, os, evento);
            case ORCAMENTO_REJEITADO -> orcamentoRejeitado(saga, os, evento);
            case ORCAMENTO_APROVADO -> orcamentoAprovado(saga, os, evento);
            case PAGAMENTO_APROVADO -> pagamentoAprovado(saga, os, evento);
            case PAGAMENTO_RECUSADO -> pagamentoRecusado(saga, os, evento);
            case EXECUCAO_ATUALIZADA -> execucaoAtualizada(saga, os, evento);
            case EXECUCAO_FINALIZADA -> execucaoFinalizada(saga, os, evento);
            case EXECUCAO_FALHOU -> execucaoFalhou(saga, os, evento);
            case ORCAMENTO_CANCELADO, PAGAMENTO_ESTORNADO -> compensacaoConfirmada(saga, evento);
            default -> false;
        };
        if (!aplicado) {
            log.warn("Evento {} não se aplica à saga {} na etapa {}, ignorado", evento.tipo(), saga.getId(), saga.getEtapa());
            saga.registrar(evento.tipo(), "Ignorado: recebido com a saga em " + saga.getEtapa());
        }
        sagas.salvar(saga);
    }

    @Override
    public int verificarPrazos() {
        List<SagaOrdemServico> vencidas = sagas.buscarComPrazoVencido(agora());
        for (SagaOrdemServico saga : vencidas) {
            OrdemServico os = carregarOS(saga.getOrdemServicoId());
            String motivo = "Prazo esgotado em " + saga.getEtapa();
            log.warn("Saga {} da OS {}: {}", saga.getId(), os.getNumero(), motivo);
            switch (saga.getEtapa()) {
                case GERANDO_ORCAMENTO -> {
                    // o Billing pode ainda criar o orçamento depois; pedimos o cancelamento por garantia
                    pedirCancelamentoDoOrcamento(saga, motivo);
                    gestorStatus.mudar(os, StatusOS.EM_DIAGNOSTICO, ORIGEM, "Billing não respondeu a tempo");
                }
                case AGUARDANDO_APROVACAO -> {
                    pedirCancelamentoDoOrcamento(saga, motivo);
                    gestorStatus.mudar(os, StatusOS.CANCELADA, ORIGEM, "Cliente não respondeu ao orçamento no prazo");
                }
                case AGUARDANDO_PAGAMENTO -> {
                    devolverEstoque(saga, os);
                    pedirCancelamentoDoOrcamento(saga, motivo);
                    gestorStatus.mudar(os, StatusOS.CANCELADA, ORIGEM, "Pagamento não realizado no prazo");
                }
                default -> {
                    continue;
                }
            }
            saga.compensar("PRAZO_ESGOTADO", motivo);
            metricas.sagaCompensada("prazo");
            sagas.salvar(saga);
        }
        return vencidas.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SagaOrdemServico> sagasDaOrdemServico(Long ordemServicoId) {
        return sagas.buscarPorOrdemServico(ordemServicoId);
    }

    // ──────────────────────────── passos ────────────────────────────

    private boolean orcamentoGerado(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.GERANDO_ORCAMENTO)) {
            return false;
        }
        saga.setOrcamentoId(evento.texto("orcamentoId"));
        saga.avancar(EtapaSaga.AGUARDANDO_APROVACAO, evento.tipo(),
                "Orçamento " + saga.getOrcamentoId() + " enviado ao cliente (R$ " + evento.texto("valorTotal") + ")",
                agora().plus(prazoAprovacao));
        gestorStatus.mudar(os, StatusOS.AGUARDANDO_APROVACAO, "billing-service", "Orçamento enviado ao cliente");
        return true;
    }

    private boolean orcamentoFalhou(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.GERANDO_ORCAMENTO)) {
            return false;
        }
        String motivo = "Billing não gerou o orçamento: " + evento.texto("motivo");
        gestorStatus.mudar(os, StatusOS.EM_DIAGNOSTICO, "billing-service", motivo);
        saga.encerrarCompensada(evento.tipo(), motivo);
        metricas.sagaCompensada("orcamento_falhou");
        return true;
    }

    private boolean orcamentoRejeitado(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_APROVACAO)) {
            return false;
        }
        String motivo = "Orçamento rejeitado pelo cliente: " + evento.texto("motivo");
        gestorStatus.mudar(os, StatusOS.EM_DIAGNOSTICO, "billing-service", motivo);
        saga.encerrarCompensada(evento.tipo(), motivo);
        metricas.sagaCompensada("orcamento_rejeitado");
        return true;
    }

    private boolean orcamentoAprovado(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_APROVACAO)) {
            return false;
        }
        saga.setLinkPagamento(evento.texto("linkPagamento"));
        try {
            estoque.reservar(os);
        } catch (EstoqueInsuficienteException e) {
            saga.registrar(evento.tipo(), "Cliente aprovou, mas a reserva de peças falhou");
            pedirCancelamentoDoOrcamento(saga, e.getMessage());
            gestorStatus.mudar(os, StatusOS.CANCELADA, ORIGEM, e.getMessage());
            saga.compensar("ESTOQUE_INSUFICIENTE", e.getMessage());
            metricas.sagaCompensada("estoque");
            return true;
        }
        saga.setEstoqueReservado(!os.getItensPeca().isEmpty());
        saga.avancar(EtapaSaga.AGUARDANDO_PAGAMENTO, evento.tipo(),
                "Orçamento aprovado, peças reservadas, aguardando pagamento", agora().plus(prazoPagamento));
        gestorStatus.mudar(os, StatusOS.AGUARDANDO_PAGAMENTO, "billing-service", "Orçamento aprovado pelo cliente");
        return true;
    }

    private boolean pagamentoAprovado(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_PAGAMENTO)) {
            return false;
        }
        saga.setPagamentoId(evento.texto("pagamentoId"));
        comandos.enviar(ComandosSagaPort.EXECUCAO, EXECUTAR_OS, saga.getId(), os.getId(), dadosExecucao(os));
        saga.avancar(EtapaSaga.AGUARDANDO_EXECUCAO, evento.tipo(),
                "Pagamento " + saga.getPagamentoId() + " confirmado, OS enviada para a fila de execução", null);
        gestorStatus.mudar(os, StatusOS.AGUARDANDO_EXECUCAO, "billing-service", "Pagamento confirmado");
        return true;
    }

    private boolean pagamentoRecusado(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_PAGAMENTO)) {
            return false;
        }
        String motivo = "Pagamento recusado: " + evento.texto("motivo");
        devolverEstoque(saga, os);
        pedirCancelamentoDoOrcamento(saga, motivo);
        gestorStatus.mudar(os, StatusOS.CANCELADA, "billing-service", motivo);
        saga.compensar(evento.tipo(), motivo);
        metricas.sagaCompensada("pagamento_recusado");
        return true;
    }

    private boolean execucaoAtualizada(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_EXECUCAO, EtapaSaga.EM_EXECUCAO)) {
            return false;
        }
        String andamento = "Execução: " + evento.texto("statusExecucao")
                + (evento.texto("responsavel") != null ? " (" + evento.texto("responsavel") + ")" : "");
        if (os.getStatus() == StatusOS.AGUARDANDO_EXECUCAO) {
            gestorStatus.mudar(os, StatusOS.EM_EXECUCAO, "execution-service", andamento);
        } else {
            gestorStatus.anotar(os, "execution-service", andamento);
        }
        saga.avancar(EtapaSaga.EM_EXECUCAO, evento.tipo(), andamento, null);
        return true;
    }

    private boolean execucaoFinalizada(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_EXECUCAO, EtapaSaga.EM_EXECUCAO)) {
            return false;
        }
        if (os.getStatus() == StatusOS.AGUARDANDO_EXECUCAO) {
            // o evento de início pode ter chegado depois (ou se perdido): passa por EM_EXECUCAO
            os = gestorStatus.mudar(os, StatusOS.EM_EXECUCAO, "execution-service", "Execução iniciada");
        }
        gestorStatus.mudar(os, StatusOS.FINALIZADA, "execution-service",
                evento.texto("observacoes") == null ? "Serviço concluído" : evento.texto("observacoes"));
        saga.avancar(EtapaSaga.CONCLUIDA, evento.tipo(), "Serviço concluído, saga finalizada", null);
        metricas.sagaConcluida();
        return true;
    }

    private boolean execucaoFalhou(SagaOrdemServico saga, OrdemServico os, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.AGUARDANDO_EXECUCAO, EtapaSaga.EM_EXECUCAO)) {
            return false;
        }
        String motivo = "Falha na execução: " + evento.texto("motivo");
        devolverEstoque(saga, os);
        Map<String, Object> dados = new HashMap<>();
        dados.put("motivo", motivo);
        comandos.enviar(ComandosSagaPort.BILLING, ESTORNAR_PAGAMENTO, saga.getId(), os.getId(), dados);
        gestorStatus.mudar(os, StatusOS.CANCELADA, "execution-service", motivo);
        saga.compensar(evento.tipo(), motivo + " — estorno do pagamento solicitado");
        metricas.sagaCompensada("execucao_falhou");
        return true;
    }

    private boolean compensacaoConfirmada(SagaOrdemServico saga, EventoSagaCommand evento) {
        if (!saga.estaEm(EtapaSaga.COMPENSANDO)) {
            return false;
        }
        String descricao = PAGAMENTO_ESTORNADO.equals(evento.tipo())
                ? "Pagamento estornado pelo Billing" : "Orçamento cancelado pelo Billing";
        saga.avancar(EtapaSaga.COMPENSADA, evento.tipo(), descricao, null);
        return true;
    }

    // ──────────────────────────── apoio ────────────────────────────

    private void pedirCancelamentoDoOrcamento(SagaOrdemServico saga, String motivo) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("motivo", motivo);
        comandos.enviar(ComandosSagaPort.BILLING, CANCELAR_ORCAMENTO, saga.getId(), saga.getOrdemServicoId(), dados);
    }

    private void devolverEstoque(SagaOrdemServico saga, OrdemServico os) {
        if (saga.isEstoqueReservado()) {
            estoque.devolver(os);
            saga.setEstoqueReservado(false);
            saga.registrar("ESTOQUE_DEVOLVIDO", "Peças reservadas voltaram para o estoque");
        }
    }

    private Map<String, Object> dadosOrcamento(OrdemServico os) {
        List<Map<String, Object>> itens = new ArrayList<>();
        for (ItemServicoOS item : os.getItensServico()) {
            itens.add(Map.of("tipo", "SERVICO", "referenciaId", item.getServico().getId(),
                    "descricao", item.getServico().getNome(), "quantidade", item.getQuantidade(),
                    "precoUnitario", item.getPrecoUnitario()));
        }
        for (ItemPecaOS item : os.getItensPeca()) {
            itens.add(Map.of("tipo", "PECA", "referenciaId", item.getPeca().getId(),
                    "descricao", item.getPeca().getNome(), "quantidade", item.getQuantidade(),
                    "precoUnitario", item.getPrecoUnitario()));
        }
        Map<String, Object> cliente = new HashMap<>();
        cliente.put("nome", os.getCliente().getNome());
        cliente.put("documento", os.getCliente().getDocumento());
        cliente.put("email", os.getCliente().getEmail());

        Map<String, Object> dados = new HashMap<>();
        dados.put("numeroOS", os.getNumero());
        dados.put("cliente", cliente);
        dados.put("itens", itens);
        dados.put("valorTotal", os.getValorTotal());
        return dados;
    }

    private Map<String, Object> dadosExecucao(OrdemServico os) {
        List<Map<String, Object>> itens = new ArrayList<>();
        os.getItensServico().forEach(i -> itens.add(Map.of("tipo", "SERVICO",
                "descricao", i.getServico().getNome(), "quantidade", i.getQuantidade())));
        os.getItensPeca().forEach(i -> itens.add(Map.of("tipo", "PECA",
                "descricao", i.getPeca().getNome(), "quantidade", i.getQuantidade())));
        Map<String, Object> dados = new HashMap<>();
        dados.put("numeroOS", os.getNumero());
        dados.put("descricaoProblema", os.getDescricaoProblema() == null ? "" : os.getDescricaoProblema());
        dados.put("itens", itens);
        dados.put("prioridade", 0);
        return dados;
    }

    private OrdemServico carregarOS(Long id) {
        return ordens.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço não encontrada: " + id));
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now();
    }
}
