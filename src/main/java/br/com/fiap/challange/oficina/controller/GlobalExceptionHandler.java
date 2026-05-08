package br.com.fiap.challange.oficina.controller;

import br.com.fiap.challange.oficina.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNotFound(RecursoNaoEncontradoException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(DocumentoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleDocumentoInvalido(DocumentoInvalidoException ex) {
        log.warn("Documento inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ErroResponse> handleEstoque(EstoqueInsuficienteException ex) {
        log.warn("Estoque insuficiente: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<ErroResponse> handleTransicao(TransicaoStatusInvalidaException ex) {
        log.warn("Transição de status inválida: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegra(RegraDeNegocioException ex) {
        log.warn("Regra de negócio violada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field = e instanceof FieldError fe ? fe.getField() : e.getObjectName();
            campos.put(field, e.getDefaultMessage());
        });
        log.warn("Validação falhou campos={}", campos);
        ErroResponse err = new ErroResponse("Dados inválidos", HttpStatus.BAD_REQUEST.value(), LocalDateTime.now(), campos);
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Credenciais inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro("Credenciais inválidas", HttpStatus.UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acesso negado");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro("Acesso negado", HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(erro("Erro interno: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private ErroResponse erro(String mensagem, HttpStatus status) {
        return new ErroResponse(mensagem, status.value(), LocalDateTime.now(), null);
    }

    public record ErroResponse(String mensagem, int status, LocalDateTime timestamp, Map<String, String> campos) {
    }
}
