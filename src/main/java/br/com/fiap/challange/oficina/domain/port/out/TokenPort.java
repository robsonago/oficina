package br.com.fiap.challange.oficina.domain.port.out;

public interface TokenPort {
    String generateToken(String username, String role);
    long getExpiration();
}
