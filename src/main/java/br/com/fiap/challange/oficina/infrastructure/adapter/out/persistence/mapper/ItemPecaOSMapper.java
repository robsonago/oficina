package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.ItemPecaOS;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.ItemPecaOSJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PecaMapper.class)
public interface ItemPecaOSMapper {

    @Mapping(target = "ordemServico", ignore = true)
    ItemPecaOSJpaEntity toEntity(ItemPecaOS domain);

    @Mapping(target = "ordemServico", ignore = true)
    ItemPecaOS toDomain(ItemPecaOSJpaEntity entity);
}
