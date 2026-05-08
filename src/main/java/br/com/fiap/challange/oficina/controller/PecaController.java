package br.com.fiap.challange.oficina.controller;

import br.com.fiap.challange.oficina.dto.request.AtualizarEstoqueRequest;
import br.com.fiap.challange.oficina.dto.request.PecaRequest;
import br.com.fiap.challange.oficina.dto.response.PecaResponse;
import br.com.fiap.challange.oficina.service.PecaService;
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

    private final PecaService pecaService;

    @PostMapping
    @Operation(summary = "Criar nova peça/insumo")
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pecaService.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar peças/insumos ativos")
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(pecaService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por ID")
    public ResponseEntity<PecaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pecaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar peça/insumo")
    public ResponseEntity<PecaResponse> atualizar(@PathVariable Long id,
                                                  @Valid @RequestBody PecaRequest request) {
        return ResponseEntity.ok(pecaService.atualizar(id, request));
    }

    @PatchMapping("/{id}/estoque")
    @Operation(summary = "Atualizar estoque de uma peça")
    public ResponseEntity<PecaResponse> atualizarEstoque(@PathVariable Long id,
                                                         @Valid @RequestBody AtualizarEstoqueRequest request) {
        return ResponseEntity.ok(pecaService.atualizarEstoque(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar peça (soft delete)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pecaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
