package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.Servico;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.ServicoJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServicoMapper {

    ServicoJpaEntity toEntity(Servico domain);

    Servico toDomain(ServicoJpaEntity entity);
}
