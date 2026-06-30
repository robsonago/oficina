package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.port.in.PecaInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.AtualizarEstoqueCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.PecaCommand;
import br.com.fiap.challange.oficina.dto.request.AtualizarEstoqueRequest;
import br.com.fiap.challange.oficina.dto.request.PecaRequest;
import br.com.fiap.challange.oficina.dto.response.PecaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pecas")
@RequiredArgsConstructor
@Tag(name = "Peças e Insumos", description = "CRUD de peças e insumos com controle de estoque")
public class PecaController {

    private final PecaInputPort pecaUseCase;

    @PostMapping
    @Operation(summary = "Criar nova peça/insumo")
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        PecaCommand command = new PecaCommand(request.nome(), request.descricao(), request.precoUnitario(), request.quantidadeEstoque(), request.codigoReferencia());
        return ResponseEntity.status(HttpStatus.CREATED).body(PecaResponse.from(pecaUseCase.criar(command)));
    }

    @GetMapping
    @Operation(summary = "Listar peças/insumos ativos")
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(pecaUseCase.listar().stream().map(PecaResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por ID")
    public ResponseEntity<PecaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(PecaResponse.from(pecaUseCase.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar peça/insumo")
    public ResponseEntity<PecaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody PecaRequest request) {
        PecaCommand command = new PecaCommand(request.nome(), request.descricao(), request.precoUnitario(), request.quantidadeEstoque(), request.codigoReferencia());
        return ResponseEntity.ok(PecaResponse.from(pecaUseCase.atualizar(id, command)));
    }

    @PatchMapping("/{id}/estoque")
    @Operation(summary = "Atualizar estoque de uma peça")
    public ResponseEntity<PecaResponse> atualizarEstoque(@PathVariable Long id, @Valid @RequestBody AtualizarEstoqueRequest request) {
        AtualizarEstoqueCommand command = new AtualizarEstoqueCommand(request.quantidade());
        return ResponseEntity.ok(PecaResponse.from(pecaUseCase.atualizarEstoque(id, command)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar peça (soft delete)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pecaUseCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
