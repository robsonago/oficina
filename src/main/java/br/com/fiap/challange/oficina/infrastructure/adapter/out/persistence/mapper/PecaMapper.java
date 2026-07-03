package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.Peca;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.PecaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PecaMapper {

    PecaJpaEntity toEntity(Peca domain);

    Peca toDomain(PecaJpaEntity entity);
}
