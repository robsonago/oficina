package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.ItemServicoOS;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.ItemServicoOSJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ServicoMapper.class)
public interface ItemServicoOSMapper {

    @Mapping(target = "ordemServico", ignore = true)
    ItemServicoOSJpaEntity toEntity(ItemServicoOS domain);

    @Mapping(target = "ordemServico", ignore = true)
    ItemServicoOS toDomain(ItemServicoOSJpaEntity entity);
}
