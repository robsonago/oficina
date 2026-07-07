package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.Veiculo;
import br.com.fiap.challange.oficina.domain.port.in.command.VeiculoCommand;

import java.util.List;

public interface VeiculoInputPort {
    Veiculo criar(VeiculoCommand command);
    List<Veiculo> listar();
    Veiculo buscarPorId(Long id);
    Veiculo buscarPorPlaca(String placa);
    List<Veiculo> listarPorCliente(Long clienteId);
    Veiculo atualizar(Long id, VeiculoCommand command);
    void deletar(Long id);
}
