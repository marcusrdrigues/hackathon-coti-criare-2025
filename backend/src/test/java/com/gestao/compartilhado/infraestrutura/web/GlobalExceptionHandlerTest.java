package com.gestao.compartilhado.infraestrutura.web;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.MuitasTentativasException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/** Respostas que os testes de API não alcançam com facilidade: erro inesperado, banco e bloqueio. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void erroInesperadoNaoExpoeDetalhes() {
        ResponseEntity<ErrorResponse> resposta = handler.inesperado(new IllegalStateException("conexão recusada pelo banco"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().getMessage()).isEqualTo(GlobalExceptionHandler.ERRO_INTERNO);
    }

    @Test
    void violacaoDeIntegridadeViraConflitoComMensagemClara() {
        assertThat(mensagem(handler.integridade(new DataIntegrityViolationException("duplicate key value"))))
                .contains("Já existe");
        assertThat(mensagem(handler.integridade(new DataIntegrityViolationException("violates foreign key"))))
                .contains("registros relacionados");
        assertThat(mensagem(handler.integridade(new DataIntegrityViolationException(null))))
                .contains("integridade");
        assertThat(handler.integridade(new DataIntegrityViolationException("x")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void acessoNegadoPorPerfilUsaMensagemPadrao() {
        assertThat(mensagem(handler.acessoNegado(new AccessDeniedException("Access Denied"))))
                .isEqualTo(GlobalExceptionHandler.ACESSO_NEGADO_POR_PERFIL);
        assertThat(mensagem(handler.acessoNegado(new AcessoNegadoException("Você não participa desta negociação."))))
                .isEqualTo("Você não participa desta negociação.");
    }

    @Test
    void bloqueioDeLoginInformaQuandoTentarDeNovo() {
        ResponseEntity<ErrorResponse> resposta = handler.muitasTentativas(
                new MuitasTentativasException("Muitas tentativas.", 90));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("90");
    }

    @Test
    void argumentoInvalidoViraRequisicaoInvalida() {
        assertThat(handler.regraDeNegocio(new IllegalArgumentException("Status desconhecido")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static String mensagem(ResponseEntity<ErrorResponse> resposta) {
        return resposta.getBody().getMessage();
    }
}
