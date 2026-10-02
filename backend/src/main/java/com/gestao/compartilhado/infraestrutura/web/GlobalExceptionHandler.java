package com.gestao.compartilhado.infraestrutura.web;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.EventoDeSeguranca;
import com.gestao.compartilhado.dominio.MuitasTentativasException;
import com.gestao.compartilhado.dominio.NaoAutenticadoException;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte as exceções em Problem Details (RFC 9457) para toda a API (ver docs/adr/0020).
 * Erros esperados (do cliente ou de regra) vão para o log como aviso; só o inesperado
 * vira erro, com a pilha completa no log e uma mensagem genérica para quem chamou.
 */
@Slf4j
@Hidden
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    static final String ACESSO_NEGADO_POR_PERFIL = "Seu perfil não tem permissão para esta ação.";
    static final String ERRO_INTERNO = "Erro interno do servidor. Tente novamente mais tarde.";

    private final ApplicationEventPublisher eventos;

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Problema> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({RegraDeNegocioException.class, IllegalArgumentException.class})
    public ResponseEntity<Problema> regraDeNegocio(RuntimeException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<Problema> naoAutenticado(NaoAutenticadoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * AccessDeniedException vem do @PreAuthorize (perfil errado); a nossa traz a mensagem do caso de uso.
     * Os dois viram evento de segurança (spec 003, R2), com a rota que foi negada.
     */
    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<Problema> acessoNegado(RuntimeException ex, HttpServletRequest requisicao) {
        String mensagem = ex instanceof AcessoNegadoException ? ex.getMessage() : ACESSO_NEGADO_POR_PERFIL;
        eventos.publishEvent(EventoDeSeguranca.daPessoaAtual(EventoDeSeguranca.Tipo.ACESSO_NEGADO,
                rota(requisicao)));
        return resposta(HttpStatus.FORBIDDEN, mensagem);
    }

    /** Método e caminho, sem a query string: é o que basta para saber o que foi tentado. */
    public static String rota(HttpServletRequest requisicao) {
        return requisicao.getMethod() + " " + requisicao.getRequestURI();
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Problema> muitasTentativas(MuitasTentativasException ex) {
        log.warn("{} {}", HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaLiberar()))
                .contentType(Problema.TIPO)
                .body(Problema.de(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Problema> duplicado(RecursoDuplicadoException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problema> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        log.warn("Erro de validação nos campos {}", erros.keySet());
        return ResponseEntity.badRequest().contentType(Problema.TIPO)
                .body(Problema.de(HttpStatus.BAD_REQUEST, "Confira os campos destacados.").comErros(erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problema> corpoInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Verifique o formato do JSON, das datas e dos valores.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Problema> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' com valor inválido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Problema> parametroAusente(MissingServletRequestParameterException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getParameterName() + "' é obrigatório.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Problema> metodoNaoPermitido(HttpRequestMethodNotSupportedException ex) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Esta rota não aceita o método " + ex.getMethod() + ".");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Problema> formatoNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo da requisição como JSON.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Problema> rotaInexistente(NoResourceFoundException ex) {
        return resposta(HttpStatus.NOT_FOUND, "Endpoint não encontrado.");
    }

    /** Corrida entre duas gravações iguais: o banco barra o que a regra não chegou a ver. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Problema> integridade(DataIntegrityViolationException ex) {
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
    public ResponseEntity<Problema> inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(Problema.TIPO)
                .body(Problema.de(HttpStatus.INTERNAL_SERVER_ERROR, ERRO_INTERNO));
    }

    private static ResponseEntity<Problema> resposta(HttpStatus status, String mensagem) {
        log.warn("{} {}", status.value(), mensagem);
        return ResponseEntity.status(status).contentType(Problema.TIPO).body(Problema.de(status, mensagem));
    }
}
