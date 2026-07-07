package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.port.in.command.ClienteCommand;

import java.util.List;

public interface ClienteInputPort {
    Cliente criar(ClienteCommand command);
    List<Cliente> listar();
    Cliente buscarPorId(Long id);
    Cliente buscarPorDocumento(String documento);
    Cliente atualizar(Long id, ClienteCommand command);
    void deletar(Long id);
}
