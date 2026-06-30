package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.Peca;
import br.com.fiap.challange.oficina.domain.port.in.command.AtualizarEstoqueCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.PecaCommand;

import java.util.List;

public interface PecaInputPort {
    Peca criar(PecaCommand command);
    List<Peca> listar();
    Peca buscarPorId(Long id);
    Peca atualizar(Long id, PecaCommand command);
    Peca atualizarEstoque(Long id, AtualizarEstoqueCommand command);
    void deletar(Long id);
}
