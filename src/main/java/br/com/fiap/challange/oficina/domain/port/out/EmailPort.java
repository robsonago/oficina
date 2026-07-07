package br.com.fiap.challange.oficina.domain.port.out;

import java.math.BigDecimal;

public interface EmailPort {
    void enviarNotificacaoOrcamento(String destinatario, String nomeCliente, String numeroOS, BigDecimal valorTotal);
}
