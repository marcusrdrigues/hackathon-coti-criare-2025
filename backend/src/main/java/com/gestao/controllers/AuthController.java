package com.gestao.controllers;

import com.gestao.dtos.auth.LoginRequest;
import com.gestao.dtos.auth.LoginResponse;
import com.gestao.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login de empresas e fornecedores")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Autentica uma empresa ou um fornecedor pelo e-mail e senha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado; o campo 'tipo' indica o perfil"),
            @ApiResponse(responseCode = "401", description = "Email ou senha inválidos")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.senha()));
    }
}
