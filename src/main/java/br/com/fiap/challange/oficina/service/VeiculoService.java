package br.com.fiap.challange.oficina.service;

import br.com.fiap.challange.oficina.dto.request.VeiculoRequest;
import br.com.fiap.challange.oficina.dto.response.VeiculoResponse;
import br.com.fiap.challange.oficina.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.model.Cliente;
import br.com.fiap.challange.oficina.model.Veiculo;
import br.com.fiap.challange.oficina.repository.ClienteRepository;
import br.com.fiap.challange.oficina.repository.VeiculoRepository;
import br.com.fiap.challange.oficina.validator.PlacaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;

    public VeiculoResponse criar(VeiculoRequest request) {
        log.info("Criando veículo placa={} clienteId={}", request.placa(), request.clienteId());
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

        VeiculoResponse response = VeiculoResponse.from(veiculoRepository.save(veiculo));
        log.info("Veículo criado id={} placa={}", response.id(), response.placa());
        return response;
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponse> listar() {
        log.info("Listando veículos");
        return veiculoRepository.findAll().stream().map(VeiculoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) {
        log.info("Buscando veículo id={}", id);
        return VeiculoResponse.from(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorPlaca(String placa) {
        log.info("Buscando veículo placa={}", placa);
        String placaNormalizada = PlacaValidator.normalizar(placa);
        return veiculoRepository.findByPlaca(placaNormalizada)
                .map(VeiculoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com placa: " + placa));
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponse> listarPorCliente(Long clienteId) {
        log.info("Listando veículos clienteId={}", clienteId);
        return veiculoRepository.findByClienteId(clienteId).stream().map(VeiculoResponse::from).toList();
    }

    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        log.info("Atualizando veículo id={}", id);
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
            Cliente cliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + request.clienteId()));
            veiculo.setCliente(cliente);
        }

        veiculo.setMarca(request.marca());
        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());

        return VeiculoResponse.from(veiculoRepository.save(veiculo));
    }

    public void deletar(Long id) {
        log.info("Desativando veículo id={}", id);
        Veiculo veiculo = buscarEntidadePorId(id);
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
    }

    public Veiculo buscarEntidadePorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));
    }
}
