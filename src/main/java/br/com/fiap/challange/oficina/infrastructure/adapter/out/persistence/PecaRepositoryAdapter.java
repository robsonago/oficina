package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.Peca;
import br.com.fiap.challange.oficina.domain.port.out.PecaRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper.PecaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PecaRepositoryAdapter implements PecaRepositoryPort {

    private final PecaJpaRepository jpaRepository;
    private final PecaMapper mapper;

    @Override
    public Peca save(Peca peca) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(peca)));
    }

    @Override
    public Optional<Peca> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Peca> findByAtivoTrue() {
        return jpaRepository.findByAtivoTrue().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
