package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.dto.request.AtualizarEstoqueRequest;
import br.com.fiap.challange.oficina.dto.request.PecaRequest;
import br.com.fiap.challange.oficina.dto.response.PecaResponse;

import java.util.List;

public interface PecaInputPort {
    PecaResponse criar(PecaRequest request);
    List<PecaResponse> listar();
    PecaResponse buscarPorId(Long id);
    PecaResponse atualizar(Long id, PecaRequest request);
    PecaResponse atualizarEstoque(Long id, AtualizarEstoqueRequest request);
    void deletar(Long id);
}
