package br.com.fiap.challange.oficina.infrastructure.messaging;

public class FalhaMensageriaException extends RuntimeException {

    public FalhaMensageriaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
