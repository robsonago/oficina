package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_processadas")
@Getter
@Setter
@NoArgsConstructor
public class MensagemProcessadaJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "processada_em", nullable = false)
    private LocalDateTime processadaEm;

    public MensagemProcessadaJpaEntity(String id, String tipo) {
        this.id = id;
        this.tipo = tipo;
        this.processadaEm = LocalDateTime.now();
    }
}
