package br.com.fiap.challange.oficina.domain.port.in.command;

import java.util.Map;

/** Evento de um participante já traduzido do formato da mensageria. */
public record EventoSagaCommand(String tipo, String sagaId, Long ordemServicoId, Map<String, Object> dados) {

    public String texto(String campo) {
        Object valor = dados == null ? null : dados.get(campo);
        return valor == null ? null : valor.toString();
    }
}
