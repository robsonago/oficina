package br.com.fiap.challange.oficina.application.usecase;

import br.com.fiap.challange.oficina.domain.model.HistoricoStatusOS;
import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.out.HistoricoStatusRepositoryPort;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Ponto único para mudar o status de uma OS: valida a transição, salva e
 * grava o histórico. Usado tanto pelos endpoints quanto pela saga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GestorStatusOS {

    private final OrdemServicoRepositoryPort osRepository;
    private final HistoricoStatusRepositoryPort historico;

    public OrdemServico mudar(OrdemServico os, StatusOS novo, String origem, String observacao) {
        StatusOS anterior = os.getStatus();
        os.transicionarStatus(novo);
        OrdemServico salva = osRepository.save(os);
        historico.registrar(new HistoricoStatusOS(null, os.getId(), anterior, novo, origem, observacao, LocalDateTime.now()));
        log.info("OS {} {} -> {} (origem={})", os.getNumero(), anterior, novo, origem);
        return salva;
    }

    /** Anota algo no histórico sem mudar o status (ex.: andamento informado pela execução). */
    public void anotar(OrdemServico os, String origem, String observacao) {
        historico.registrar(new HistoricoStatusOS(null, os.getId(), os.getStatus(), os.getStatus(), origem,
                observacao, LocalDateTime.now()));
    }
}
