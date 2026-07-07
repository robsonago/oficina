package br.com.fiap.challange.oficina.infrastructure.adapter.out.email;

import br.com.fiap.challange.oficina.domain.port.out.EmailPort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.host")
public class JavaMailSenderEmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${mail.from:oficina@localhost}")
    private String remetente;

    @Override
    public void enviarNotificacaoOrcamento(String destinatario, String nomeCliente,
                                           String numeroOS, BigDecimal valorTotal) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("E-mail não enviado para OS {}: cliente sem e-mail cadastrado", numeroOS);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("Orçamento disponível - OS " + numeroOS);
            helper.setText(montarCorpo(nomeCliente, numeroOS, valorTotal), true);
            mailSender.send(message);
            log.info("E-mail de orçamento enviado de={} para={} OS={}", remetente, destinatario, numeroOS);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {} OS={}: {}", destinatario, numeroOS, e.getMessage());
        }
    }

    private String montarCorpo(String nomeCliente, String numeroOS, BigDecimal valorTotal) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                  <h2 style="color: #333;">Orçamento disponível para aprovação</h2>
                  <p>Olá, <strong>%s</strong>!</p>
                  <p>O orçamento da sua Ordem de Serviço <strong>%s</strong> foi gerado e está aguardando sua aprovação.</p>
                  <table style="border-collapse: collapse; width: 100%%;">
                    <tr style="background-color: #f2f2f2;">
                      <td style="padding: 8px; border: 1px solid #ddd;"><strong>OS</strong></td>
                      <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 8px; border: 1px solid #ddd;"><strong>Valor Total</strong></td>
                      <td style="padding: 8px; border: 1px solid #ddd;">R$ %s</td>
                    </tr>
                  </table>
                  <p style="margin-top: 20px;">Entre em contato com a oficina para <strong>aprovar ou recusar</strong> o orçamento.</p>
                  <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                  <p style="color: #999; font-size: 12px;">Oficina Mecânica — Sistema de Gestão</p>
                </body>
                </html>
                """.formatted(nomeCliente, numeroOS, numeroOS, valorTotal.toPlainString());
    }
}
