package br.com.fiap.challange.oficina.domain.port.in.command;

public record VeiculoCommand(
        String placa,
        String marca,
        String modelo,
        Integer ano,
        Long clienteId
) {}
