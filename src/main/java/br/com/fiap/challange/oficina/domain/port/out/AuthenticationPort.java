package br.com.fiap.challange.oficina.domain.port.out;

public interface AuthenticationPort {
    void autenticar(String username, String password);
}
