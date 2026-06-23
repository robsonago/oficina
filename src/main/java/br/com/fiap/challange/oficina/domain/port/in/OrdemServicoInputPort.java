package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.dto.request.ItemPecaRequest;
import br.com.fiap.challange.oficina.dto.request.ItemServicoRequest;
import br.com.fiap.challange.oficina.dto.request.OrdemServicoRequest;
import br.com.fiap.challange.oficina.dto.response.EstatisticasResponse;
import br.com.fiap.challange.oficina.dto.response.OrdemServicoResponse;

import java.util.List;

public interface OrdemServicoInputPort {
    OrdemServicoResponse criar(OrdemServicoRequest request);
    List<OrdemServicoResponse> listar();
    OrdemServicoResponse buscarPorId(Long id);
    List<OrdemServicoResponse> listarPorStatus(StatusOS status);
    String consultarStatus(Long id);
    OrdemServicoResponse iniciarDiagnostico(Long id);
    OrdemServicoResponse gerarOrcamento(Long id);
    OrdemServicoResponse aprovarOrcamento(Long id);
    OrdemServicoResponse rejeitarOrcamento(Long id);
    OrdemServicoResponse finalizar(Long id);
    OrdemServicoResponse entregar(Long id);
    OrdemServicoResponse adicionarServico(Long osId, ItemServicoRequest request);
    OrdemServicoResponse adicionarPeca(Long osId, ItemPecaRequest request);
    EstatisticasResponse calcularEstatisticas();
}
