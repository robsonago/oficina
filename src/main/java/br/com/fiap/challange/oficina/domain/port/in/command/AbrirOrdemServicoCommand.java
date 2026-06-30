package br.com.fiap.challange.oficina.domain.port.in.command;

import java.util.List;

public record AbrirOrdemServicoCommand(
        String documentoCliente,
        String placaVeiculo,
        String descricaoProblema,
        String observacoes,
        List<AdicionarItemServicoCommand> servicos,
        List<AdicionarItemPecaCommand> pecas
) {}
