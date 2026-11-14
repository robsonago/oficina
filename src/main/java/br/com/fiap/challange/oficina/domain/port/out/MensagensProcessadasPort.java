package br.com.fiap.challange.oficina.domain.port.out;

public interface MensagensProcessadasPort {

    boolean jaProcessada(String mensagemId);

    void registrar(String mensagemId, String tipo);
}
