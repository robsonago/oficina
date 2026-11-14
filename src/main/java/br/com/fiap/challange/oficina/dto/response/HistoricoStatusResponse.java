package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.HistoricoStatusOS;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;

import java.time.LocalDateTime;

public record HistoricoStatusResponse(StatusOS statusAnterior, StatusOS statusNovo, String descricao, String origem,
                                      String observacao, LocalDateTime ocorridoEm) {

    public static HistoricoStatusResponse from(HistoricoStatusOS h) {
        return new HistoricoStatusResponse(h.statusAnterior(), h.statusNovo(), h.statusNovo().getDescricao(),
                h.origem(), h.observacao(), h.ocorridoEm());
    }
}
