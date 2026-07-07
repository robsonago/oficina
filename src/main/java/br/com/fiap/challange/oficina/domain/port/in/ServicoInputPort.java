package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.Servico;
import br.com.fiap.challange.oficina.domain.port.in.command.ServicoCommand;

import java.util.List;

public interface ServicoInputPort {
    Servico criar(ServicoCommand command);
    List<Servico> listar();
    Servico buscarPorId(Long id);
    Servico atualizar(Long id, ServicoCommand command);
    void deletar(Long id);
}
