package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.port.in.ServicoInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.ServicoCommand;
import br.com.fiap.challange.oficina.dto.request.ServicoRequest;
import br.com.fiap.challange.oficina.dto.response.ServicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "CRUD de serviços da oficina")
public class ServicoController {

    private final ServicoInputPort servicoUseCase;

    @PostMapping
    @Operation(summary = "Criar novo serviço")
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        ServicoCommand command = new ServicoCommand(request.nome(), request.descricao(), request.preco(), request.tempoEstimadoMinutos());
        return ResponseEntity.status(HttpStatus.CREATED).body(ServicoResponse.from(servicoUseCase.criar(command)));
    }

    @GetMapping
    @Operation(summary = "Listar serviços ativos")
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(servicoUseCase.listar().stream().map(ServicoResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    public ResponseEntity<ServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ServicoResponse.from(servicoUseCase.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    public ResponseEntity<ServicoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        ServicoCommand command = new ServicoCommand(request.nome(), request.descricao(), request.preco(), request.tempoEstimadoMinutos());
        return ResponseEntity.ok(ServicoResponse.from(servicoUseCase.atualizar(id, command)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar serviço (soft delete)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        servicoUseCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
