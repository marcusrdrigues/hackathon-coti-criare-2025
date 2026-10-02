package com.gestao.compartilhado.infraestrutura.web;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.MuitasTentativasException;
import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte as exceções no mesmo JSON de erro para toda a API. Erros esperados (do
 * cliente ou de regra) vão para o log como aviso; só o inesperado vira erro, com a
 * pilha completa no log e uma mensagem genérica para quem chamou.
 */
@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String ACESSO_NEGADO_POR_PERFIL = "Seu perfil não tem permissão para esta ação.";
    static final String ERRO_INTERNO = "Erro interno do servidor. Tente novamente mais tarde.";

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({RegraDeNegocioException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> regraDeNegocio(RuntimeException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ErrorResponse> naoAutenticado(NaoAutenticadoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** AccessDeniedException vem do @PreAuthorize (perfil errado); a nossa traz a mensagem do caso de uso. */
    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> acessoNegado(RuntimeException ex) {
        String mensagem = ex instanceof AcessoNegadoException ? ex.getMessage() : ACESSO_NEGADO_POR_PERFIL;
        return resposta(HttpStatus.FORBIDDEN, mensagem);
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErrorResponse> muitasTentativas(MuitasTentativasException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaLiberar()))
                .body(corpo(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(RecursoDuplicadoException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            erros.put(campo.getField(), campo.getDefaultMessage());
        }
        log.warn("Erro de validação nos campos {}", erros.keySet());
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(), "Erro de validação nos campos", LocalDateTime.now(), erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> corpoInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Verifique o formato do JSON, das datas e dos valores.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' com valor inválido.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rotaInexistente(NoResourceFoundException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Endpoint não encontrado.");
    }

    /** Corrida entre duas gravações iguais: o banco barra o que a regra não chegou a ver. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridade(DataIntegrityViolationException ex) {
        String detalhe = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String mensagem;
        if (detalhe.contains("unique") || detalhe.contains("duplicate key")) {
            mensagem = "Já existe um registro com estes dados no sistema.";
        } else if (detalhe.contains("foreign key") || detalhe.contains("referential integrity")) {
            mensagem = "Não é possível realizar esta operação pois existem registros relacionados.";
        } else {
            mensagem = "Erro de integridade de dados. Verifique se não há registros duplicados ou relacionamentos inválidos.";
        }
        return resposta(HttpStatus.CONFLICT, mensagem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(corpo(HttpStatus.INTERNAL_SERVER_ERROR, ERRO_INTERNO));
    }

    private static ResponseEntity<ErrorResponse> resposta(HttpStatus status, String mensagem) {
        log.warn("{} {}", status.value(), mensagem);
        return ResponseEntity.status(status).body(corpo(status, mensagem));
    }

    private static ErrorResponse corpo(HttpStatus status, String mensagem) {
        return new ErrorResponse(status.value(), mensagem, LocalDateTime.now());
    }
}
