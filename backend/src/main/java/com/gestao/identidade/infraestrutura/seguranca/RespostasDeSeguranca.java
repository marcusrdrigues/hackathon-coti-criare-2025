package com.gestao.identidade.infraestrutura.seguranca;

import com.gestao.compartilhado.infraestrutura.web.Problema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas 401/403 geradas pelos filtros do Spring Security (antes de chegar
 * ao controller), no mesmo formato Problem Details do GlobalExceptionHandler.
 */
@Component
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        String mensagem = request.getHeader(HttpHeaders.AUTHORIZATION) == null
                ? "Faça login para acessar este recurso."
                : "Token inválido ou expirado.";
        escrever(response, HttpStatus.UNAUTHORIZED, mensagem);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta ação.");
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(Problema.TIPO.toString());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(Problema.de(status, mensagem).paraJson());
    }
}
