package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.PassoSaga;
import br.com.fiap.challange.oficina.domain.model.SagaOrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;
import br.com.fiap.challange.oficina.domain.port.out.SagaRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.SagaJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SagaRepositoryAdapter implements SagaRepositoryPort {

    private static final EnumSet<EtapaSaga> ENCERRADAS = EnumSet.of(EtapaSaga.CONCLUIDA, EtapaSaga.COMPENSADA);
    private static final EnumSet<EtapaSaga> COM_PRAZO = EnumSet.of(EtapaSaga.GERANDO_ORCAMENTO,
            EtapaSaga.AGUARDANDO_APROVACAO, EtapaSaga.AGUARDANDO_PAGAMENTO);

    private final SagaJpaRepository jpa;

    @Override
    public SagaOrdemServico salvar(SagaOrdemServico saga) {
        SagaJpaEntity entity = jpa.findById(saga.getId()).orElseGet(SagaJpaEntity::new);
        entity.setId(saga.getId());
        entity.setOrdemServicoId(saga.getOrdemServicoId());
        entity.setEtapa(saga.getEtapa());
        entity.setOrcamentoId(saga.getOrcamentoId());
        entity.setPagamentoId(saga.getPagamentoId());
        entity.setLinkPagamento(saga.getLinkPagamento());
        entity.setEstoqueReservado(saga.isEstoqueReservado());
        entity.setMotivoCompensacao(saga.getMotivoCompensacao());
        entity.setPrazo(saga.getPrazo());
        entity.setCriadaEm(saga.getCriadaEm());
        entity.setAtualizadaEm(saga.getAtualizadaEm());
        entity.getPassos().clear();
        for (PassoSaga passo : saga.getPassos()) {
            SagaJpaEntity.PassoEmbeddable emb = new SagaJpaEntity.PassoEmbeddable();
            emb.setEtapa(passo.etapa());
            emb.setEvento(passo.evento());
            emb.setDescricao(passo.descricao());
            emb.setOcorridoEm(passo.ocorridoEm());
            entity.getPassos().add(emb);
        }
        return paraDominio(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<SagaOrdemServico> buscarPorId(String id) {
        return jpa.findById(id).map(SagaRepositoryAdapter::paraDominio);
    }

    @Override
    public List<SagaOrdemServico> buscarPorOrdemServico(Long ordemServicoId) {
        return jpa.findByOrdemServicoIdOrderByCriadaEmDesc(ordemServicoId).stream()
                .map(SagaRepositoryAdapter::paraDominio).toList();
    }

    @Override
    public Optional<SagaOrdemServico> buscarAtivaPorOrdemServico(Long ordemServicoId) {
        return jpa.findFirstByOrdemServicoIdAndEtapaNotInOrderByCriadaEmDesc(ordemServicoId, ENCERRADAS)
                .map(SagaRepositoryAdapter::paraDominio);
    }

    @Override
    public List<SagaOrdemServico> buscarComPrazoVencido(LocalDateTime agora) {
        return jpa.findByEtapaInAndPrazoBefore(COM_PRAZO, agora).stream()
                .map(SagaRepositoryAdapter::paraDominio).toList();
    }

    private static SagaOrdemServico paraDominio(SagaJpaEntity e) {
        return SagaOrdemServico.builder()
                .id(e.getId())
                .ordemServicoId(e.getOrdemServicoId())
                .etapa(e.getEtapa())
                .orcamentoId(e.getOrcamentoId())
                .pagamentoId(e.getPagamentoId())
                .linkPagamento(e.getLinkPagamento())
                .estoqueReservado(e.isEstoqueReservado())
                .motivoCompensacao(e.getMotivoCompensacao())
                .prazo(e.getPrazo())
                .criadaEm(e.getCriadaEm())
                .atualizadaEm(e.getAtualizadaEm())
                .versao(e.getVersao())
                .passos(new ArrayList<>(e.getPassos().stream()
                        .map(p -> new PassoSaga(p.getEtapa(), p.getEvento(), p.getDescricao(), p.getOcorridoEm()))
                        .toList()))
                .build();
    }
}
