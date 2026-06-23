package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.dto.request.ServicoRequest;
import br.com.fiap.challange.oficina.dto.response.ServicoResponse;

import java.util.List;

public interface ServicoInputPort {
    ServicoResponse criar(ServicoRequest request);
    List<ServicoResponse> listar();
    ServicoResponse buscarPorId(Long id);
    ServicoResponse atualizar(Long id, ServicoRequest request);
    void deletar(Long id);
}
