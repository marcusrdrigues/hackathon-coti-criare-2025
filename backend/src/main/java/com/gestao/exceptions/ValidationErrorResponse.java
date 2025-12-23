package com.gestao.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Resposta de erro de validação")
public class ValidationErrorResponse {

    @Schema(description = "Código de status HTTP", example = "400")
    private int status;

    @Schema(description = "Mensagem de erro", example = "Erro de validação")
    private String message;

    @Schema(description = "Timestamp do erro", example = "2025-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Mapa de erros de validação por campo",
            example = "{\"email\": \"Email é obrigatório\", \"senha\": \"Senha deve ter no mínimo 6 caracteres\"}")
    private Map<String, String> errors;
}