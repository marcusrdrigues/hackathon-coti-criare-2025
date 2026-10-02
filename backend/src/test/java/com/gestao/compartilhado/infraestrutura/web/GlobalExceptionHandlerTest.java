package com.gestao.compartilhado.infraestrutura.web;

import com.gestao.compartilhado.dominio.AcessoNegadoException;
import com.gestao.compartilhado.dominio.EventoDeSeguranca;
import com.gestao.compartilhado.dominio.MuitasTentativasException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Respostas que os testes de API não alcançam com facilidade: erro inesperado, banco e bloqueio. */
class GlobalExceptionHandlerTest {

    private final List<Object> publicados = new ArrayList<>();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(publicados::add);

    @Test
    void erroInesperadoNaoExpoeDetalhes() {
        ResponseEntity<Problema> resposta = handler.inesperado(new IllegalStateException("conexão recusada pelo banco"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().detail()).isEqualTo(GlobalExceptionHandler.ERRO_INTERNO);
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
        MockHttpServletRequest requisicao = new MockHttpServletRequest("PATCH", "/api/v1/negociacoes/1/fechar");
        requisicao.setQueryString("valor=10");
        assertThat(mensagem(handler.acessoNegado(new AccessDeniedException("Access Denied"), requisicao)))
                .isEqualTo(GlobalExceptionHandler.ACESSO_NEGADO_POR_PERFIL);
        String doCasoDeUso = "Só a empresa desta negociação pode fechá-la ou encerrá-la.";
        assertThat(mensagem(handler.acessoNegado(new AcessoNegadoException(doCasoDeUso), requisicao)))
                .isEqualTo(doCasoDeUso);

        // Cada 403 vira um evento de segurança, com a rota e sem a query string
        assertThat(publicados).hasSize(2).allSatisfy(evento -> {
            assertThat(evento).isInstanceOf(EventoDeSeguranca.class);
            assertThat(((EventoDeSeguranca) evento).tipo()).isEqualTo(EventoDeSeguranca.Tipo.ACESSO_NEGADO);
            assertThat(((EventoDeSeguranca) evento).detalhe()).isEqualTo("PATCH /api/v1/negociacoes/1/fechar");
        });
    }

    @Test
    void bloqueioDeLoginInformaQuandoTentarDeNovo() {
        ResponseEntity<Problema> resposta = handler.muitasTentativas(
                new MuitasTentativasException("Muitas tentativas.", 90));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("90");
    }

    @Test
    void problemaTemTituloStatusEDetalheNoFormatoDaRfc() {
        ResponseEntity<Problema> resposta = handler.naoEncontrado(
                new RecursoNaoEncontradoException("Cotação não encontrada!"));

        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        Problema problema = resposta.getBody();
        assertThat(problema.type()).isEqualTo("about:blank");
        assertThat(problema.title()).isEqualTo("Não encontrado");
        assertThat(problema.status()).isEqualTo(404);
        assertThat(problema.detail()).isEqualTo("Cotação não encontrada!");
        assertThat(problema.erros()).isNull();
    }

    @Test
    void jsonDosFiltrosDeSegurancaEscapaOTexto() {
        String json = new Problema("about:blank", "Acesso negado", 403, "Diz \"não\"\nlinha", null, "abc", null)
                .paraJson();

        assertThat(json).isEqualTo("{\"type\":\"about:blank\",\"title\":\"Acesso negado\",\"status\":403,"
                + "\"detail\":\"Diz \\\"não\\\"\\nlinha\",\"traceId\":\"abc\"}");
    }

    @Test
    void argumentoInvalidoViraRequisicaoInvalida() {
        assertThat(handler.regraDeNegocio(new IllegalArgumentException("Status desconhecido")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static String mensagem(ResponseEntity<Problema> resposta) {
        return resposta.getBody().detail();
    }
}
