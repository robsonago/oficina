package br.com.fiap.challange.oficina.controller;

import br.com.fiap.challange.oficina.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import br.com.fiap.challange.oficina.infrastructure.adapter.in.rest.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void deveRetornar404ParaRecursoNaoEncontrado() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleNotFound(new RecursoNaoEncontradoException("não encontrado"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().mensagem()).contains("não encontrado");
    }

    @Test
    void deveRetornar400ParaDocumentoInvalido() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleDocumentoInvalido(new DocumentoInvalidoException("CPF inválido"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deveRetornar422ParaEstoqueInsuficiente() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleEstoque(new EstoqueInsuficienteException("sem estoque"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void deveRetornar422ParaTransicaoInvalida() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleTransicao(new TransicaoStatusInvalidaException("transição inválida"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void deveRetornar400ParaRegraDeNegocio() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleRegra(new RegraDeNegocioException("regra violada"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deveRetornar401ParaCredenciaisInvalidas() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("bad creds"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRetornar403ParaAcessoNegado() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("negado"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveRetornar500ParaErroGenerico() {
        ResponseEntity<GlobalExceptionHandler.ErroResponse> response =
                handler.handleGeneric(new RuntimeException("erro inesperado"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().mensagem()).contains("erro inesperado");
    }

    @Test
    void deveRetornar400ParaValidacaoComErroNaoCampo() throws Exception {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        ObjectError objectError = new ObjectError("meuObjeto", "campo obrigatório");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(objectError));

        ResponseEntity<GlobalExceptionHandler.ErroResponse> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().campos()).containsKey("meuObjeto");
    }
}
