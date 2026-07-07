package br.com.fiap.challange.oficina.domain.port.out;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
}
