package br.com.fiap.challange.oficina.domain.model;

import br.com.fiap.challange.oficina.domain.model.enums.EtapaSaga;

import java.time.LocalDateTime;

/** Registro de cada passo da saga, para auditoria e para mostrar o fluxo na demonstração. */
public record PassoSaga(EtapaSaga etapa, String evento, String descricao, LocalDateTime ocorridoEm) {
}
