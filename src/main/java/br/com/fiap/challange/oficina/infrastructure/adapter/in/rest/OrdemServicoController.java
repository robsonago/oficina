package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.OrdemServicoInputPort;
import br.com.fiap.challange.oficina.domain.port.in.OrquestradorSagaInputPort;
import br.com.fiap.challange.oficina.domain.port.out.AcompanhamentoExternoPort;
import br.com.fiap.challange.oficina.domain.port.in.command.AbrirOrdemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemPecaCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemServicoCommand;
import br.com.fiap.challange.oficina.dto.request.ItemPecaRequest;
import br.com.fiap.challange.oficina.dto.request.ItemServicoRequest;
import br.com.fiap.challange.oficina.dto.request.OrdemServicoRequest;
import br.com.fiap.challange.oficina.dto.response.AcompanhamentoResponse;
import br.com.fiap.challange.oficina.dto.response.EstatisticasResponse;
import br.com.fiap.challange.oficina.dto.response.HistoricoStatusResponse;
import br.com.fiap.challange.oficina.dto.response.OrdemServicoResponse;
import br.com.fiap.challange.oficina.dto.response.SagaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Gestão completa das ordens de serviço")
public class OrdemServicoController {

    private final OrdemServicoInputPort osUseCase;
    private final OrquestradorSagaInputPort orquestrador;
    private final AcompanhamentoExternoPort acompanhamentoExterno;

    @PostMapping
    @Operation(summary = "Criar nova Ordem de Serviço")
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody OrdemServicoRequest request) {
        List<AdicionarItemServicoCommand> servicos = request.servicos() == null ? List.of() :
                request.servicos().stream().map(i -> new AdicionarItemServicoCommand(i.servicoId(), i.quantidade())).toList();
        List<AdicionarItemPecaCommand> pecas = request.pecas() == null ? List.of() :
                request.pecas().stream().map(i -> new AdicionarItemPecaCommand(i.pecaId(), i.quantidade())).toList();
        AbrirOrdemServicoCommand command = new AbrirOrdemServicoCommand(
                request.documentoCliente(), request.placaVeiculo(),
                request.descricaoProblema(), request.observacoes(), servicos, pecas);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrdemServicoResponse.from(osUseCase.criar(command)));
    }

    @GetMapping
    @Operation(summary = "Listar OS ativas ordenadas por prioridade (da mais adiantada para a mais nova), excluindo FINALIZADA, ENTREGUE e CANCELADA")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(osUseCase.listar().stream().map(OrdemServicoResponse::from).toList());
    }

    @GetMapping("/ativas")
    @Operation(summary = "Listar OS ativas ordenadas por prioridade (da mais adiantada para a mais nova), excluindo FINALIZADA, ENTREGUE e CANCELADA")
    public ResponseEntity<List<OrdemServicoResponse>> listarAtivas() {
        return ResponseEntity.ok(osUseCase.listarAtivas().stream().map(OrdemServicoResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Ordem de Serviço por ID")
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.buscarPorId(id)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar OS por status")
    public ResponseEntity<List<OrdemServicoResponse>> listarPorStatus(@PathVariable StatusOS status) {
        return ResponseEntity.ok(osUseCase.listarPorStatus(status).stream().map(OrdemServicoResponse::from).toList());
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Consultar status da OS (acesso público para o cliente)")
    public ResponseEntity<Map<String, String>> consultarStatus(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "status", osUseCase.consultarStatus(id),
                "ordemServicoId", id.toString()));
    }

    @PostMapping("/{id}/iniciar-diagnostico")
    @Operation(summary = "Iniciar diagnóstico (RECEBIDA → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.iniciarDiagnostico(id, auth.getName())));
    }

    @PostMapping("/{id}/gerar-orcamento")
    @Operation(summary = "Inicia a saga da OS: pede o orçamento ao Billing Service (EM_DIAGNOSTICO → AGUARDANDO_ORCAMENTO). "
            + "Aprovação, pagamento e execução acontecem nos outros serviços e voltam como eventos")
    public ResponseEntity<OrdemServicoResponse> gerarOrcamento(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(OrdemServicoResponse.from(osUseCase.gerarOrcamento(id, auth.getName())));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Registrar entrega do veículo (FINALIZADA → ENTREGUE)")
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.entregar(id, auth.getName())));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico de status da OS, com a origem de cada mudança (usuário ou serviço)")
    public ResponseEntity<List<HistoricoStatusResponse>> historico(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.historico(id).stream().map(HistoricoStatusResponse::from).toList());
    }

    @GetMapping("/{id}/saga")
    @Operation(summary = "Sagas da OS (mais recente primeiro), com cada passo e compensação executados")
    public ResponseEntity<List<SagaResponse>> saga(@PathVariable Long id) {
        osUseCase.buscarPorId(id);
        return ResponseEntity.ok(orquestrador.sagasDaOrdemServico(id).stream().map(SagaResponse::from).toList());
    }

    @GetMapping("/{id}/acompanhamento")
    @Operation(summary = "Visão consolidada: OS + histórico + saga + orçamento (Billing) + execução (Execution), "
            + "consultados via REST com timeout e circuit breaker")
    public ResponseEntity<AcompanhamentoResponse> acompanhamento(@PathVariable Long id) {
        OrdemServicoResponse os = OrdemServicoResponse.from(osUseCase.buscarPorId(id));
        List<String> indisponiveis = new ArrayList<>();
        Object orcamentos = acompanhamentoExterno.orcamentos(id).orElseGet(() -> {
            indisponiveis.add("billing-service");
            return null;
        });
        Object execucoes = acompanhamentoExterno.execucoes(id).orElseGet(() -> {
            indisponiveis.add("execution-service");
            return null;
        });
        return ResponseEntity.ok(new AcompanhamentoResponse(os,
                osUseCase.historico(id).stream().map(HistoricoStatusResponse::from).toList(),
                orquestrador.sagasDaOrdemServico(id).stream().map(SagaResponse::from).toList(),
                orcamentos, execucoes, indisponiveis));
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarServico(@PathVariable Long id,
                                                                 @Valid @RequestBody ItemServicoRequest request) {
        AdicionarItemServicoCommand command = new AdicionarItemServicoCommand(request.servicoId(), request.quantidade());
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.adicionarServico(id, command)));
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(@PathVariable Long id,
                                                              @Valid @RequestBody ItemPecaRequest request) {
        AdicionarItemPecaCommand command = new AdicionarItemPecaCommand(request.pecaId(), request.quantidade());
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.adicionarPeca(id, command)));
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas das ordens de serviço e tempo médio de execução")
    public ResponseEntity<EstatisticasResponse> estatisticas() {
        return ResponseEntity.ok(EstatisticasResponse.from(osUseCase.calcularEstatisticas()));
    }
}
