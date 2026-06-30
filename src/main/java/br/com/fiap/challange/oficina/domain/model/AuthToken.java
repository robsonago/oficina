package br.com.fiap.challange.oficina.domain.model;

public record AuthToken(String token, String tipo, String username, String role, Long expiresIn) {}
