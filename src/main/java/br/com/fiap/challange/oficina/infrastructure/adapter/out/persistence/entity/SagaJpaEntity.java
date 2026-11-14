package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity;

import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sagas_ordem_servico")
@Getter
@Setter
public class SagaJpaEntity {

    @Id
    private String id;

    @Column(name = "ordem_servico_id", nullable = false)
    private Long ordemServicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EtapaSaga etapa;

    @Column(name = "orcamento_id", length = 64)
    private String orcamentoId;

    @Column(name = "pagamento_id", length = 64)
    private String pagamentoId;

    @Column(name = "link_pagamento", columnDefinition = "TEXT")
    private String linkPagamento;

    @Column(name = "estoque_reservado", nullable = false)
    private boolean estoqueReservado;

    @Column(name = "motivo_compensacao", columnDefinition = "TEXT")
    private String motivoCompensacao;

    private LocalDateTime prazo;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    /** Duas réplicas tratando eventos da mesma saga ao mesmo tempo: uma perde e a mensagem é reentregue. */
    @Version
    private Long versao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "saga_passos", joinColumns = @JoinColumn(name = "saga_id"))
    @OrderColumn(name = "posicao")
    private List<PassoEmbeddable> passos = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    public static class PassoEmbeddable {
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private EtapaSaga etapa;
        @Column(nullable = false, length = 50)
        private String evento;
        @Column(columnDefinition = "TEXT")
        private String descricao;
        @Column(name = "ocorrido_em", nullable = false)
        private LocalDateTime ocorridoEm;
    }
}
