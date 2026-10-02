package com.gestao.compartilhado.infraestrutura.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Resposta de erro padrão da API")
public class ErrorResponse {

    @Schema(description = "Código de status HTTP", example = "404")
    private int status;

    @Schema(description = "Mensagem de erro", example = "Recurso não encontrado")
    private String message;

    @Schema(description = "Timestamp do erro", example = "2025-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Identificador de rastreio da requisição, o mesmo dos logs e do cabeçalho X-Trace-Id",
            example = "4bf92f3577b34da6a3ce929d0e0e4736")
    private String traceId;
}