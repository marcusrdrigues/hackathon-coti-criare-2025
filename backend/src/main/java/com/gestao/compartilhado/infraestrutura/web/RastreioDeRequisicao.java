package com.gestao.compartilhado.infraestrutura.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Garante um identificador de rastreio em toda requisição e o devolve no cabeçalho
 * {@value Rastreio#CABECALHO}, inclusive nas respostas de erro do Spring Security.
 *
 * <p>Com o OpenTelemetry ativo, o identificador é o traceId do trace da requisição, que o
 * Micrometer Tracing já colocou no MDC. Sem ele (por exemplo, nos testes), este filtro gera
 * um no mesmo formato. O valor nunca vem do cliente, para ninguém injetar texto nos logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // depois do filtro de observação do Spring, antes da segurança
public class RastreioDeRequisicao extends OncePerRequestFilter {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = Rastreio.idAtual();
        boolean gerado = id == null;
        if (gerado) {
            id = novoId();
            MDC.put(Rastreio.CHAVE, id);
        }
        response.setHeader(Rastreio.CABECALHO, id);
        try {
            chain.doFilter(request, response);
        } finally {
            if (gerado) {
                MDC.remove(Rastreio.CHAVE);
            }
        }
    }

    /** 16 bytes em hexadecimal: o formato de traceId do W3C Trace Context. */
    static String novoId() {
        byte[] bytes = new byte[16];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
