package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.ItemPecaOS;

import java.math.BigDecimal;

public record ItemPecaOSResponse(
        Long id,
        Long pecaId,
        String nomePeca,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ItemPecaOSResponse from(ItemPecaOS i) {
        return new ItemPecaOSResponse(
                i.getId(),
                i.getPeca().getId(),
                i.getPeca().getNome(),
                i.getQuantidade(),
                i.getPrecoUnitario(),
                i.getSubtotal()
        );
    }
}
