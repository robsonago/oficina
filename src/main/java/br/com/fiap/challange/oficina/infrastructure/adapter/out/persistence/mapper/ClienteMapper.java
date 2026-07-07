package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.ClienteJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteJpaEntity toEntity(Cliente domain);

    Cliente toDomain(ClienteJpaEntity entity);
}
