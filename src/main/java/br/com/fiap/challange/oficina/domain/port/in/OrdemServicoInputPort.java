package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.Estatisticas;
import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.in.command.AbrirOrdemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemPecaCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AdicionarItemServicoCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.AprovacaoOrcamentoCommand;

import java.util.List;

public interface OrdemServicoInputPort {
    OrdemServico criar(AbrirOrdemServicoCommand command);
    List<OrdemServico> listar();
    List<OrdemServico> listarAtivas();
    OrdemServico buscarPorId(Long id);
    List<OrdemServico> listarPorStatus(StatusOS status);
    String consultarStatus(Long id);
    OrdemServico iniciarDiagnostico(Long id);
    OrdemServico gerarOrcamento(Long id);
    OrdemServico aprovarOrcamento(Long id);
    OrdemServico rejeitarOrcamento(Long id);
    OrdemServico aprovarOuRejeitarOrcamento(Long id, AprovacaoOrcamentoCommand command);
    OrdemServico finalizar(Long id);
    OrdemServico entregar(Long id);
    OrdemServico adicionarServico(Long osId, AdicionarItemServicoCommand command);
    OrdemServico adicionarPeca(Long osId, AdicionarItemPecaCommand command);
    Estatisticas calcularEstatisticas();
}
