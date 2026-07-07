package br.com.fiap.challange.oficina.domain.port.in.command;

import java.math.BigDecimal;

public record ServicoCommand(
        String nome,
        String descricao,
        BigDecimal preco,
        Integer tempoEstimadoMinutos
) {}
