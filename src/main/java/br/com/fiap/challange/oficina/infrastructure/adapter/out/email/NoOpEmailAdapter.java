package br.com.fiap.challange.oficina.infrastructure.adapter.out.email;

import br.com.fiap.challange.oficina.domain.port.out.EmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@ConditionalOnMissingBean(JavaMailSenderEmailAdapter.class)
public class NoOpEmailAdapter implements EmailPort {

    @Override
    public void enviarNotificacaoOrcamento(String destinatario, String nomeCliente,
                                           String numeroOS, BigDecimal valorTotal) {
        log.info("[NoOp] E-mail de orçamento ignorado (sem SMTP configurado) - destinatario={} OS={}", destinatario, numeroOS);
    }
}
