package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.port.out.MensagensProcessadasPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.MensagemProcessadaJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MensagensProcessadasAdapter implements MensagensProcessadasPort {

    private final MensagemProcessadaJpaRepository repository;

    @Override
    public boolean jaProcessada(String mensagemId) {
        return repository.existsById(mensagemId);
    }

    @Override
    public void registrar(String mensagemId, String tipo) {
        // gravado na mesma transação do processamento: se duas réplicas
        // processarem a mesma mensagem ao mesmo tempo, a chave primária
        // derruba uma delas e o Pub/Sub reentrega (e aí já consta como processada)
        repository.save(new MensagemProcessadaJpaEntity(mensagemId, tipo));
    }
}
