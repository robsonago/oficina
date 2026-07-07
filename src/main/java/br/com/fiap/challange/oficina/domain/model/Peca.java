package br.com.fiap.challange.oficina.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Peca {

    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario;

    @Builder.Default
    private Integer quantidadeEstoque = 0;

    private String codigoReferencia;

    @Builder.Default
    private Boolean ativo = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
