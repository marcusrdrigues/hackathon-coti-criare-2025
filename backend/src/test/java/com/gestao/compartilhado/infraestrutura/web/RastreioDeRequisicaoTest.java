package com.gestao.compartilhado.infraestrutura.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** O filtro sozinho: quando não há trace aberto, ele cria o id; quando há, só repassa. */
class RastreioDeRequisicaoTest {

    private final RastreioDeRequisicao filtro = new RastreioDeRequisicao();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void semTraceAbertoGeraUmIdQueValeDuranteARequisicao() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        AtomicReference<String> durante = new AtomicReference<>();

        filtro.doFilter(new MockHttpServletRequest(), resposta,
                new MockFilterChain(new HttpServlet() {
                    @Override
                    protected void service(HttpServletRequest req, HttpServletResponse res) {
                        durante.set(Rastreio.idAtual());
                    }
                }));

        assertThat(resposta.getHeader(Rastreio.CABECALHO)).matches("[0-9a-f]{32}").isEqualTo(durante.get());
        assertThat(Rastreio.idAtual()).as("o id não pode vazar para a próxima requisição da thread").isNull();
    }

    @Test
    void comTraceAbertoUsaOIdDoTraceENaoMexeNoMdc() throws Exception {
        MDC.put(Rastreio.CHAVE, "4bf92f3577b34da6a3ce929d0e0e4736");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest(), resposta, new MockFilterChain());

        assertThat(resposta.getHeader(Rastreio.CABECALHO)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(Rastreio.idAtual()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void idsGeradosNaoSeRepetem() {
        assertThat(RastreioDeRequisicao.novoId()).isNotEqualTo(RastreioDeRequisicao.novoId());
    }
}
