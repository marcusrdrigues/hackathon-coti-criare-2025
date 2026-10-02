package com.gestao.compartilhado.infraestrutura.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Corpo de todo erro da API, no formato Problem Details (RFC 9457), com o
 * identificador de rastreio e, nos erros de validação, a mensagem de cada campo
 * (ver docs/adr/0020). Vai com o tipo {@code application/problem+json}.
 */
@Schema(description = "Erro no formato Problem Details (RFC 9457)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problema(
        @Schema(description = "Tipo do problema; about:blank quando o status já explica", example = "about:blank")
        String type,
        @Schema(description = "Resumo do tipo de problema", example = "Não encontrado")
        String title,
        @Schema(description = "Código de status HTTP", example = "404")
        int status,
        @Schema(description = "O que aconteceu nesta requisição, para mostrar a quem usa",
                example = "Cotação não encontrada!")
        String detail,
        @Schema(description = "Caminho da requisição", example = "/api/v1/cotacoes/0b7c…")
        String instance,
        @Schema(description = "Identificador de rastreio, o mesmo dos logs e do cabeçalho X-Trace-Id",
                example = "4bf92f3577b34da6a3ce929d0e0e4736")
        String traceId,
        @Schema(description = "Erros de validação por campo",
                example = "{\"email\": \"Email é obrigatório\"}")
        Map<String, String> erros) {

    public static final MediaType TIPO = MediaType.APPLICATION_PROBLEM_JSON;
    static final String SEM_TIPO = "about:blank";

    /** O problema da requisição em andamento (caminho e rastreio vêm dela). */
    public static Problema de(HttpStatus status, String detalhe) {
        return new Problema(SEM_TIPO, titulo(status), status.value(), detalhe, caminhoAtual(), Rastreio.idAtual(), null);
    }

    public Problema comErros(Map<String, String> errosPorCampo) {
        return new Problema(type, title, status, detail, instance, traceId,
                Collections.unmodifiableMap(new LinkedHashMap<>(errosPorCampo)));
    }

    /** O mesmo JSON que o Jackson gera, para quem responde fora do Spring MVC (os filtros de segurança). */
    public String paraJson() {
        StringBuilder json = new StringBuilder("{");
        campo(json, "type", type);
        campo(json, "title", title);
        json.append("\"status\":").append(status).append(',');
        campo(json, "detail", detail);
        campo(json, "instance", instance);
        campo(json, "traceId", traceId);
        json.setLength(json.length() - 1);
        return json.append('}').toString();
    }

    static String titulo(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Requisição inválida";
            case UNAUTHORIZED -> "Não autenticado";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            case CONFLICT -> "Conflito";
            case UNSUPPORTED_MEDIA_TYPE -> "Formato não suportado";
            case TOO_MANY_REQUESTS -> "Muitas tentativas";
            case INTERNAL_SERVER_ERROR -> "Erro interno";
            default -> status.getReasonPhrase();
        };
    }

    private static String caminhoAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRequestURI();
        }
        return null;
    }

    private static void campo(StringBuilder json, String nome, String valor) {
        if (valor != null) {
            json.append('"').append(nome).append("\":\"").append(escapar(valor)).append("\",");
        }
    }

    private static String escapar(String texto) {
        StringBuilder saida = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    if (c < 0x20) {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        return saida.toString();
    }
}
