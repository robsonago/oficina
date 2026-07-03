package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.OrdemServicoInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.AbrirOrdemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemPecaCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AprovacaoOrcamentoCommand;
import br.com.fiap.challange.oficina.dto.request.AprovacaoOrcamentoRequest;
import br.com.fiap.challange.oficina.dto.request.ItemPecaRequest;
import br.com.fiap.challange.oficina.dto.request.ItemServicoRequest;
import br.com.fiap.challange.oficina.dto.request.OrdemServicoRequest;
import br.com.fiap.challange.oficina.dto.response.EstatisticasResponse;
import br.com.fiap.challange.oficina.dto.response.OrdemServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço", description = "Gestão completa das ordens de serviço")
public class OrdemServicoController {

    private final OrdemServicoInputPort osUseCase;

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
    @Operation(summary = "Listar OS ativas ordenadas por prioridade (EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA), excluindo FINALIZADA e ENTREGUE")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(osUseCase.listar().stream().map(OrdemServicoResponse::from).toList());
    }

    @GetMapping("/ativas")
    @Operation(summary = "Listar OS ativas ordenadas por prioridade (EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA), excluindo FINALIZADA e ENTREGUE")
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
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable Long id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.iniciarDiagnostico(id)));
    }

    @PostMapping("/{id}/gerar-orcamento")
    @Operation(summary = "Gerar orçamento e enviar para aprovação (EM_DIAGNOSTICO → AGUARDANDO_APROVACAO)")
    public ResponseEntity<OrdemServicoResponse> gerarOrcamento(@PathVariable Long id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.gerarOrcamento(id)));
    }

    @PostMapping("/{id}/aprovacao-orcamento")
    @Operation(summary = "Aprovar ou rejeitar orçamento via body (aprovado: true → EM_EXECUCAO, false → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> aprovacaoOrcamento(@PathVariable Long id,
                                                                   @Valid @RequestBody AprovacaoOrcamentoRequest request) {
        AprovacaoOrcamentoCommand command = new AprovacaoOrcamentoCommand(request.aprovado(), request.observacao());
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.aprovarOuRejeitarOrcamento(id, command)));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar execução dos serviços (EM_EXECUCAO → FINALIZADA)")
    public ResponseEntity<OrdemServicoResponse> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.finalizar(id)));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Registrar entrega do veículo (FINALIZADA → ENTREGUE)")
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable Long id) {
        return ResponseEntity.ok(OrdemServicoResponse.from(osUseCase.entregar(id)));
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
