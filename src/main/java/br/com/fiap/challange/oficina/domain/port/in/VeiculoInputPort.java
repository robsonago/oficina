package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.dto.request.VeiculoRequest;
import br.com.fiap.challange.oficina.dto.response.VeiculoResponse;

import java.util.List;

public interface VeiculoInputPort {
    VeiculoResponse criar(VeiculoRequest request);
    List<VeiculoResponse> listar();
    VeiculoResponse buscarPorId(Long id);
    VeiculoResponse buscarPorPlaca(String placa);
    List<VeiculoResponse> listarPorCliente(Long clienteId);
    VeiculoResponse atualizar(Long id, VeiculoRequest request);
    void deletar(Long id);
}
