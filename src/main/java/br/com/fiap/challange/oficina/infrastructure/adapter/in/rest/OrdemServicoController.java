package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.OrdemServicoInputPort;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(osUseCase.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar todas as Ordens de Serviço")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(osUseCase.listar());
    }

    @GetMapping("/ativas")
    @Operation(summary = "Listar OS ativas ordenadas por prioridade (EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA), excluindo FINALIZADA e ENTREGUE")
    public ResponseEntity<List<OrdemServicoResponse>> listarAtivas() {
        return ResponseEntity.ok(osUseCase.listarAtivas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Ordem de Serviço por ID")
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.buscarPorId(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar OS por status")
    public ResponseEntity<List<OrdemServicoResponse>> listarPorStatus(@PathVariable StatusOS status) {
        return ResponseEntity.ok(osUseCase.listarPorStatus(status));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Consultar status da OS (acesso público para o cliente)")
    public ResponseEntity<Map<String, String>> consultarStatus(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "status", osUseCase.consultarStatus(id),
                "ordemServicoId", id.toString()
        ));
    }

    @PostMapping("/{id}/iniciar-diagnostico")
    @Operation(summary = "Iniciar diagnóstico (RECEBIDA → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.iniciarDiagnostico(id));
    }

    @PostMapping("/{id}/gerar-orcamento")
    @Operation(summary = "Gerar orçamento e enviar para aprovação (EM_DIAGNOSTICO → AGUARDANDO_APROVACAO)")
    public ResponseEntity<OrdemServicoResponse> gerarOrcamento(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.gerarOrcamento(id));
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar orçamento (AGUARDANDO_APROVACAO → EM_EXECUCAO)")
    public ResponseEntity<OrdemServicoResponse> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.aprovarOrcamento(id));
    }

    @PostMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeitar orçamento - retorna para diagnóstico (AGUARDANDO_APROVACAO → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> rejeitar(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.rejeitarOrcamento(id));
    }

    @PostMapping("/{id}/aprovacao-orcamento")
    @Operation(summary = "Aprovar ou rejeitar orçamento via body (aprovado: true → EM_EXECUCAO, false → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> aprovacaoOrcamento(@PathVariable Long id,
                                                                   @Valid @RequestBody AprovacaoOrcamentoRequest request) {
        return ResponseEntity.ok(osUseCase.aprovarOuRejeitarOrcamento(id, request));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar execução dos serviços (EM_EXECUCAO → FINALIZADA)")
    public ResponseEntity<OrdemServicoResponse> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.finalizar(id));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Registrar entrega do veículo (FINALIZADA → ENTREGUE)")
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable Long id) {
        return ResponseEntity.ok(osUseCase.entregar(id));
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarServico(@PathVariable Long id,
                                                                 @Valid @RequestBody ItemServicoRequest request) {
        return ResponseEntity.ok(osUseCase.adicionarServico(id, request));
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(@PathVariable Long id,
                                                              @Valid @RequestBody ItemPecaRequest request) {
        return ResponseEntity.ok(osUseCase.adicionarPeca(id, request));
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas das ordens de serviço e tempo médio de execução")
    public ResponseEntity<EstatisticasResponse> estatisticas() {
        return ResponseEntity.ok(osUseCase.calcularEstatisticas());
    }
}
