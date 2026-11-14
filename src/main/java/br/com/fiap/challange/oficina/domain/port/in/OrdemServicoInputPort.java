package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.Estatisticas;
import br.com.fiap.challange.oficina.domain.model.HistoricoStatusOS;
import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.command.AbrirOrdemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemPecaCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemServicoCommand;

import java.util.List;

public interface OrdemServicoInputPort {
    OrdemServico criar(AbrirOrdemServicoCommand command);
    List<OrdemServico> listar();
    List<OrdemServico> listarAtivas();
    OrdemServico buscarPorId(Long id);
    List<OrdemServico> listarPorStatus(StatusOS status);
    String consultarStatus(Long id);
    OrdemServico iniciarDiagnostico(Long id, String responsavel);
    /** Inicia a saga: pede o orçamento ao Billing. */
    OrdemServico gerarOrcamento(Long id, String responsavel);
    OrdemServico entregar(Long id, String responsavel);
    OrdemServico adicionarServico(Long osId, AdicionarItemServicoCommand command);
    OrdemServico adicionarPeca(Long osId, AdicionarItemPecaCommand command);
    List<HistoricoStatusOS> historico(Long id);
    Estatisticas calcularEstatisticas();
}
