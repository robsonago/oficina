package br.com.fiap.challange.oficina.domain.port.out;

import java.util.Map;

/** Comandos que o orquestrador envia aos participantes da saga. */
public interface ComandosSagaPort {

    String BILLING = "billing-comandos";
    String EXECUCAO = "execucao-comandos";

    void enviar(String destino, String tipo, String sagaId, Long ordemServicoId, Map<String, Object> dados);
}
