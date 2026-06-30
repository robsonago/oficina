package br.com.fiap.challange.oficina.infrastructure.adapter.in.rest;

import br.com.fiap.challange.oficina.domain.port.in.ClienteInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.ClienteCommand;
import br.com.fiap.challange.oficina.dto.request.ClienteRequest;
import br.com.fiap.challange.oficina.dto.response.ClienteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "CRUD de clientes")
public class ClienteController {

    private final ClienteInputPort clienteUseCase;

    @PostMapping
    @Operation(summary = "Criar novo cliente")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        ClienteCommand command = new ClienteCommand(request.nome(), request.documento(), request.email(), request.telefone(), request.endereco());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(clienteUseCase.criar(command)));
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes")
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteUseCase.listar().stream().map(ClienteResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ClienteResponse.from(clienteUseCase.buscarPorId(id)));
    }

    @GetMapping("/documento/{documento}")
    @Operation(summary = "Buscar cliente por CPF/CNPJ")
    public ResponseEntity<ClienteResponse> buscarPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(ClienteResponse.from(clienteUseCase.buscarPorDocumento(documento)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        ClienteCommand command = new ClienteCommand(request.nome(), request.documento(), request.email(), request.telefone(), request.endereco());
        return ResponseEntity.ok(ClienteResponse.from(clienteUseCase.atualizar(id, command)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente (soft delete)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        clienteUseCase.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
