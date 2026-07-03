package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.domain.model.enums.StatusOS;
import br.com.fiap.challange.oficina.domain.port.out.OrdemServicoRepositoryPort;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper.OrdemServicoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class OrdemServicoRepositoryAdapter implements OrdemServicoRepositoryPort {

    private final OrdemServicoJpaRepository jpaRepository;
    private final OrdemServicoMapper mapper;

    @Override
    public OrdemServico save(OrdemServico ordemServico) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(ordemServico)));
    }

    @Override
    public Optional<OrdemServico> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OrdemServico> findByNumero(String numero) {
        return jpaRepository.findByNumero(numero).map(mapper::toDomain);
    }

    @Override
    public List<OrdemServico> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findByStatus(StatusOS status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findByStatusIn(List<StatusOS> statuses) {
        return jpaRepository.findByStatusIn(statuses).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findByClienteId(Long clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findByVeiculoId(Long veiculoId) {
        return jpaRepository.findByVeiculoId(veiculoId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrdemServico> findFinalizadasComTempo() {
        return jpaRepository.findFinalizadasComTempo().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
