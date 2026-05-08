package br.com.fiap.challange.oficina.controller;

import br.com.fiap.challange.oficina.dto.request.ItemPecaRequest;
import br.com.fiap.challange.oficina.dto.request.ItemServicoRequest;
import br.com.fiap.challange.oficina.dto.request.OrdemServicoRequest;
import br.com.fiap.challange.oficina.dto.response.EstatisticasResponse;
import br.com.fiap.challange.oficina.dto.response.OrdemServicoResponse;
import br.com.fiap.challange.oficina.model.enums.StatusOS;
import br.com.fiap.challange.oficina.service.OrdemServicoService;
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

    private final OrdemServicoService osService;

    @PostMapping
    @Operation(summary = "Criar nova Ordem de Serviço")
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody OrdemServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(osService.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar todas as Ordens de Serviço")
    public ResponseEntity<List<OrdemServicoResponse>> listar() {
        return ResponseEntity.ok(osService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Ordem de Serviço por ID")
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(osService.buscarPorId(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Listar OS por status")
    public ResponseEntity<List<OrdemServicoResponse>> listarPorStatus(@PathVariable StatusOS status) {
        return ResponseEntity.ok(osService.listarPorStatus(status));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Consultar status da OS (acesso público para o cliente)")
    public ResponseEntity<Map<String, String>> consultarStatus(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "status", osService.consultarStatus(id),
                "ordemServicoId", id.toString()
        ));
    }

    @PostMapping("/{id}/iniciar-diagnostico")
    @Operation(summary = "Iniciar diagnóstico (RECEBIDA → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> iniciarDiagnostico(@PathVariable Long id) {
        return ResponseEntity.ok(osService.iniciarDiagnostico(id));
    }

    @PostMapping("/{id}/gerar-orcamento")
    @Operation(summary = "Gerar orçamento e enviar para aprovação (EM_DIAGNOSTICO → AGUARDANDO_APROVACAO)")
    public ResponseEntity<OrdemServicoResponse> gerarOrcamento(@PathVariable Long id) {
        return ResponseEntity.ok(osService.gerarOrcamento(id));
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar orçamento (AGUARDANDO_APROVACAO → EM_EXECUCAO)")
    public ResponseEntity<OrdemServicoResponse> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(osService.aprovarOrcamento(id));
    }

    @PostMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeitar orçamento - retorna para diagnóstico (AGUARDANDO_APROVACAO → EM_DIAGNOSTICO)")
    public ResponseEntity<OrdemServicoResponse> rejeitar(@PathVariable Long id) {
        return ResponseEntity.ok(osService.rejeitarOrcamento(id));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar execução dos serviços (EM_EXECUCAO → FINALIZADA)")
    public ResponseEntity<OrdemServicoResponse> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(osService.finalizar(id));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Registrar entrega do veículo (FINALIZADA → ENTREGUE)")
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable Long id) {
        return ResponseEntity.ok(osService.entregar(id));
    }

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarServico(@PathVariable Long id,
                                                                 @Valid @RequestBody ItemServicoRequest request) {
        return ResponseEntity.ok(osService.adicionarServico(id, request));
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS")
    public ResponseEntity<OrdemServicoResponse> adicionarPeca(@PathVariable Long id,
                                                              @Valid @RequestBody ItemPecaRequest request) {
        return ResponseEntity.ok(osService.adicionarPeca(id, request));
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas das ordens de serviço e tempo médio de execução")
    public ResponseEntity<EstatisticasResponse> estatisticas() {
        return ResponseEntity.ok(osService.calcularEstatisticas());
    }
}
