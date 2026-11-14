package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_mensagens")
@Getter
@Setter
public class OutboxJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String destino;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "saga_id", length = 64)
    private String sagaId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(columnDefinition = "TEXT")
    private String cabecalhos;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "publicada_em")
    private LocalDateTime publicadaEm;

    @Column(nullable = false)
    private int tentativas;
}
