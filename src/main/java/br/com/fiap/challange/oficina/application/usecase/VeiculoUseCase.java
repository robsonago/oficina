package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.oficina.domain.exception.RegraDeNegocioException;
import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.model.Veiculo;
import br.com.fiap.challange.oficina.domain.port.in.VeiculoInputPort;
import br.com.fiap.challange.oficina.domain.port.in.command.VeiculoCommand;
import br.com.fiap.challange.oficina.domain.port.out.ClienteRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.VeiculoRepositoryPort;
import br.com.fiap.challange.oficina.domain.validator.PlacaValidator;
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
    public Veiculo criar(VeiculoCommand command) {
        String placaNormalizada = PlacaValidator.normalizar(command.placa());
        if (!PlacaValidator.isValida(placaNormalizada)) {
            throw new RegraDeNegocioException("Placa inválida: " + command.placa());
        }
        if (veiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new RegraDeNegocioException("Já existe um veículo com a placa: " + placaNormalizada);
        }
        Cliente cliente = clienteRepository.findById(command.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + command.clienteId()));

        Veiculo veiculo = Veiculo.builder()
                .placa(placaNormalizada)
                .marca(command.marca())
                .modelo(command.modelo())
                .ano(command.ano())
                .cliente(cliente)
                .build();

        return veiculoRepository.save(veiculo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Veiculo> listar() {
        return veiculoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Veiculo buscarPorId(Long id) {
        return buscarEntidadePorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Veiculo buscarPorPlaca(String placa) {
        String placaNormalizada = PlacaValidator.normalizar(placa);
        return veiculoRepository.findByPlaca(placaNormalizada)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com placa: " + placa));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Veiculo> listarPorCliente(Long clienteId) {
        return veiculoRepository.findByClienteId(clienteId);
    }

    @Override
    public Veiculo atualizar(Long id, VeiculoCommand command) {
        Veiculo veiculo = buscarEntidadePorId(id);
        String placaNormalizada = PlacaValidator.normalizar(command.placa());

        if (!veiculo.getPlaca().equals(placaNormalizada)) {
            if (!PlacaValidator.isValida(placaNormalizada)) {
                throw new RegraDeNegocioException("Placa inválida: " + command.placa());
            }
            if (veiculoRepository.existsByPlaca(placaNormalizada)) {
                throw new RegraDeNegocioException("Já existe um veículo com a placa: " + placaNormalizada);
            }
            veiculo.setPlaca(placaNormalizada);
        }

        if (!veiculo.getCliente().getId().equals(command.clienteId())) {
            Cliente novoCliente = clienteRepository.findById(command.clienteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + command.clienteId()));
            veiculo.setCliente(novoCliente);
        }

        veiculo.setMarca(command.marca());
        veiculo.setModelo(command.modelo());
        veiculo.setAno(command.ano());

        return veiculoRepository.save(veiculo);
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
