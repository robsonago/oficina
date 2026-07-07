package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.Veiculo;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.VeiculoJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ClienteMapper.class)
public interface VeiculoMapper {

    VeiculoJpaEntity toEntity(Veiculo domain);

    Veiculo toDomain(VeiculoJpaEntity entity);
}
