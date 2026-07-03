package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.OrdemServico;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.OrdemServicoJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring",
        uses = {ClienteMapper.class, VeiculoMapper.class, ItemServicoOSMapper.class, ItemPecaOSMapper.class})
public interface OrdemServicoMapper {

    OrdemServicoJpaEntity toEntity(OrdemServico domain);

    OrdemServico toDomain(OrdemServicoJpaEntity entity);

    @AfterMapping
    default void linkItensEntity(@MappingTarget OrdemServicoJpaEntity entity) {
        entity.getItensServico().forEach(item -> item.setOrdemServico(entity));
        entity.getItensPeca().forEach(item -> item.setOrdemServico(entity));
    }

    @AfterMapping
    default void linkItensDomain(@MappingTarget OrdemServico domain) {
        domain.getItensServico().forEach(item -> item.setOrdemServico(domain));
        domain.getItensPeca().forEach(item -> item.setOrdemServico(domain));
    }
}
