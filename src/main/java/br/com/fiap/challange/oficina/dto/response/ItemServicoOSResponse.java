package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.ItemServicoOS;

import java.math.BigDecimal;

public record ItemServicoOSResponse(
        Long id,
        Long servicoId,
        String nomeServico,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ItemServicoOSResponse from(ItemServicoOS i) {
        return new ItemServicoOSResponse(
                i.getId(),
                i.getServico().getId(),
                i.getServico().getNome(),
                i.getQuantidade(),
                i.getPrecoUnitario(),
                i.getSubtotal()
        );
    }
}
