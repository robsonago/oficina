package br.com.fiap.challange.oficina.domain.port.in.command;

import java.math.BigDecimal;

public record PecaCommand(
        String nome,
        String descricao,
        BigDecimal precoUnitario,
        Integer quantidadeEstoque,
        String codigoReferencia
) {}
