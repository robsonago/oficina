package br.com.fiap.challange.oficina.dto.response;

import br.com.fiap.challange.oficina.domain.model.Cliente;
import br.com.fiap.challange.oficina.domain.model.enums.TipoDocumento;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String documento,
        TipoDocumento tipoDocumento,
        String email,
        String telefone,
        String endereco,
        Boolean ativo,
        LocalDateTime createdAt
) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getNome(), c.getDocumento(), c.getTipoDocumento(),
                c.getEmail(), c.getTelefone(), c.getEndereco(), c.getAtivo(), c.getCreatedAt()
        );
    }
}
