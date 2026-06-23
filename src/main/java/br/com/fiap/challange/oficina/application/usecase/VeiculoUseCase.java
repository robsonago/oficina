package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.model.Veiculo;
import br.com.fiap.challange.oficina.domain.port.in.VeiculoInputPort;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
import br.com.fiap.challange.oficina.domain.validator.PlacaValidator;
import br.com.fiap.challange.oficina.dto.request.VeiculoRequest;
import br.com.fiap.challange.oficina.dto.response.VeiculoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VeiculoUseCase implements VeiculoInputPort {

    private final VeiculoRepositoryPort veiculoRepository;
    private final ClienteRepositoryPort clienteRepository;

    @Override
    public VeiculoResponse criar(VeiculoRequest request) {
        String placaNormalizada = PlacaValidator.normalizar(request.placa());
        if (!PlacaValidator.isValida(placaNormalizada)) {
            throw new RegraDeNegocioException("Placa inválida: " + request.placa());
        }
        if (veiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RegraDeNegocioException("Já existe um veículo com a placa: " + placaNormalizada);
        }
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + request.clienteId()));

        Veiculo veiculo = Veiculo.builder()
                .placa(placaNormalizada)
                .marca(request.marca())
                .modelo(request.modelo())
                .ano(request.ano())
                .cliente(cliente)
                .build();

        return VeiculoResponse.from(veiculoRepository.save(veiculo));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listar() {
        return veiculoRepository.findAll().stream()
                .map(VeiculoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) {
        return VeiculoResponse.from(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorPlaca(String placa) {
        String placaNormalizada = PlacaValidator.normalizar(placa);
        return veiculoRepository.findByPlaca(placaNormalizada)
                .map(VeiculoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com placa: " + placa));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarPorCliente(Long clienteId) {
        return veiculoRepository.findByClienteId(clienteId).stream()
                .map(VeiculoResponse::from)
                .toList();
    }

    @Override
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        Veiculo veiculo = buscarEntidadePorId(id);
        String placaNormalizada = PlacaValidator.normalizar(request.placa());

        if (!veiculo.getPlaca().equals(placaNormalizada)) {
            if (!PlacaValidator.isValida(placaNormalizada)) {
                throw new RegraDeNegocioException("Placa inválida: " + request.placa());
            }
            if (veiculoRepository.existsByPlaca(placaNormalizada)) {
                throw new RegraDeNegocioException("Já existe um veículo com a placa: " + placaNormalizada);
            }
            veiculo.setPlaca(placaNormalizada);
        }

        if (!veiculo.getCliente().getId().equals(request.clienteId())) {
            Cliente novoCliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + request.clienteId()));
            veiculo.setCliente(novoCliente);
        }

        veiculo.setMarca(request.marca());
        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());

        return VeiculoResponse.from(veiculoRepository.save(veiculo));
    }

    @Override
    public void deletar(Long id) {
        Veiculo veiculo = buscarEntidadePorId(id);
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
    }

    private Veiculo buscarEntidadePorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));
    }
}
