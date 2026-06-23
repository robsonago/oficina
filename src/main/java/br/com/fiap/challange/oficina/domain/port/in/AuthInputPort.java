package br.com.fiap.challange.oficina.domain.port.in;

import br.com.fiap.challange.oficina.dto.request.LoginRequest;
import br.com.fiap.challange.oficina.dto.request.UsuarioRequest;
import br.com.fiap.challange.oficina.dto.response.LoginResponse;

public interface AuthInputPort {
    LoginResponse login(LoginRequest request);
    void registrar(UsuarioRequest request);
}
