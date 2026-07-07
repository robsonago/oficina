package br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.mapper;

import br.com.fiap.challange.oficina.domain.model.Usuario;
import br.com.fiap.challange.oficina.infrastructure.adapter.out.persistence.entity.UsuarioJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioJpaEntity toEntity(Usuario domain);

    Usuario toDomain(UsuarioJpaEntity entity);
}
