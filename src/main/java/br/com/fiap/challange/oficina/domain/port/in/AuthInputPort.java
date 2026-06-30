package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.domain.model.AuthToken;
import br.com.fiap.challange.oficina.domain.port.in.command.LoginCommand;
import br.com.fiap.challange.oficina.domain.port.in.command.RegistrarUsuarioCommand;

public interface AuthInputPort {
    AuthToken login(LoginCommand command);
    void registrar(RegistrarUsuarioCommand command);
}
