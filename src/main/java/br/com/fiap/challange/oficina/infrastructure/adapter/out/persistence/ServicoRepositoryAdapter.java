package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.Servico;
import br.com.fiap.challange.oficina.domain.port.out.ServicoRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper.ServicoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ServicoRepositoryAdapter implements ServicoRepositoryPort {

    private final ServicoJpaRepository jpaRepository;
    private final ServicoMapper mapper;

    @Override
    public Servico save(Servico servico) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(servico)));
    }

    @Override
    public Optional<Servico> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Servico> findByAtivoTrue() {
        return jpaRepository.findByAtivoTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
