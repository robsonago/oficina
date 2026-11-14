package br.com.fiap.challange.oficina.infrastructure.messaging;

import java.util.Map;

/**
 * Saída "crua" para o broker. Quem publica eventos de negócio usa os ports
 * do domínio; esta interface só existe para trocar Pub/Sub por uma
 * implementação em memória nos testes.
 */
public interface CanalMensageria {

    void enviar(String destino, Mensagem mensagem, Map<String, String> cabecalhos);
}
