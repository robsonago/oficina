package br.com.fiap.challange.oficina.infrastructure.messaging;

/**
 * Quem trata as mensagens que chegam pela assinatura do serviço.
 * Lançar exceção significa "não consegui processar": a mensagem volta para
 * a fila e, depois de algumas tentativas, vai para a dead letter.
 */
public interface ReceptorMensagens {

    void receber(Mensagem mensagem);
}
