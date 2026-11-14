package br.com.fiap.challange.oficina.infrastructure.adapter.out.rest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "servicos")
public class ServicosExternosProperties {

    /** URL interna do Billing Service (no cluster: http://oficina-billing). */
    private String billingUrl = "http://localhost:8081";

    /** URL interna do Execution Service (no cluster: http://oficina-execution). */
    private String execucaoUrl = "http://localhost:8082";

    private int timeoutMs = 2000;
}
