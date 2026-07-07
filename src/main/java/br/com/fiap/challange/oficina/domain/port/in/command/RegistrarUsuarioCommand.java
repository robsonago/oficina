package br.com.fiap.challange.oficina.domain.port.in.command;

public record RegistrarUsuarioCommand(String username, String password, String role) {}
