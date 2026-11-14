package br.com.fiap.challange.oficina.infrastructure.config;

import br.com.fiap.challange.oficina.infrastructure.adapter.out.rest.ServicosExternosProperties;
import br.com.fiap.challange.oficina.infrastructure.messaging.MensageriaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({MensageriaProperties.class, ServicosExternosProperties.class})
public class MensageriaConfig {
}
