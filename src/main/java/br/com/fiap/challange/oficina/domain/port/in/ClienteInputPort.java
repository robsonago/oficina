package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.dto.request.ClienteRequest;
import br.com.fiap.challange.oficina.dto.response.ClienteResponse;

import java.util.List;

public interface ClienteInputPort {
    ClienteResponse criar(ClienteRequest request);
    List<ClienteResponse> listar();
    ClienteResponse buscarPorId(Long id);
    ClienteResponse buscarPorDocumento(String documento);
    ClienteResponse atualizar(Long id, ClienteRequest request);
    void deletar(Long id);
}
