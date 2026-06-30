package br.com.fiap.challange.oficina.domain.port.in.command;

public record ClienteCommand(
        String nome,
        String documento,
        String email,
        String telefone,
        String endereco
) {}
