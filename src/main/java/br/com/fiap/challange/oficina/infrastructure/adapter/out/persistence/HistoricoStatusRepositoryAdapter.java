package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.HistoricoStatusOS;
import br.com.fiap.challange.oficina.domain.port.out.HistoricoStatusRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.HistoricoStatusJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class HistoricoStatusRepositoryAdapter implements HistoricoStatusRepositoryPort {

    private final HistoricoStatusJpaRepository jpa;

    @Override
    public void registrar(HistoricoStatusOS h) {
        HistoricoStatusJpaEntity e = new HistoricoStatusJpaEntity();
        e.setOrdemServicoId(h.ordemServicoId());
        e.setStatusAnterior(h.statusAnterior());
        e.setStatusNovo(h.statusNovo());
        e.setOrigem(h.origem());
        e.setObservacao(h.observacao());
        e.setOcorridoEm(h.ocorridoEm());
        jpa.save(e);
    }

    @Override
    public List<HistoricoStatusOS> buscarPorOrdemServico(Long ordemServicoId) {
        return jpa.findByOrdemServicoIdOrderByOcorridoEmAscIdAsc(ordemServicoId).stream()
                .map(e -> new HistoricoStatusOS(e.getId(), e.getOrdemServicoId(), e.getStatusAnterior(),
                        e.getStatusNovo(), e.getOrigem(), e.getObservacao(), e.getOcorridoEm()))
                .toList();
    }
}
