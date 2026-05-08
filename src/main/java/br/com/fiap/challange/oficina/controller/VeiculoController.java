package br.com.fiap.challange.oficina.controller;

import br.com.fiap.challange.oficina.dto.request.VeiculoRequest;
import br.com.fiap.challange.oficina.dto.response.VeiculoResponse;
import br.com.fiap.challange.oficina.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "CRUD de veículos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    @PostMapping
    @Operation(summary = "Cadastrar novo veículo")
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(veiculoService.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar todos os veículos")
    public ResponseEntity<List<VeiculoResponse>> listar() {
        return ResponseEntity.ok(veiculoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public ResponseEntity<VeiculoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Buscar veículo por placa")
    public ResponseEntity<VeiculoResponse> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(veiculoService.buscarPorPlaca(placa));
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listar veículos por cliente")
    public ResponseEntity<List<VeiculoResponse>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(veiculoService.listarPorCliente(clienteId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo")
    public ResponseEntity<VeiculoResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody VeiculoRequest request) {
        return ResponseEntity.ok(veiculoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar veículo (soft delete)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        veiculoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
