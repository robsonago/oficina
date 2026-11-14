package br.com.fiap.challange.oficina.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Guarda as mensagens em memória em vez de mandar para o Pub/Sub.
 * Usado nos testes (para conferir o que foi publicado) e para subir o
 * serviço sozinho na máquina sem depender do emulador.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mensageria.provider", havingValue = "memoria", matchIfMissing = true)
public class CanalEmMemoria implements CanalMensageria {

    public record Enviada(String destino, Mensagem mensagem, Map<String, String> cabecalhos) {
    }

    private final List<Enviada> enviadas = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(String destino, Mensagem mensagem, Map<String, String> cabecalhos) {
        log.info("Mensagem {} guardada em memória destino={} saga={}", mensagem.tipo(), destino, mensagem.sagaId());
        enviadas.add(new Enviada(destino, mensagem, Map.copyOf(cabecalhos)));
    }

    public List<Enviada> enviadas() {
        return List.copyOf(enviadas);
    }

    public List<Mensagem> doTipo(String tipo) {
        return enviadas.stream().map(Enviada::mensagem).filter(m -> m.tipo().equals(tipo)).toList();
    }

    public void limpar() {
        enviadas.clear();
    }
}
