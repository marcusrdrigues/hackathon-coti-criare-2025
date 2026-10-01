package com.gestao.mappers;

import com.gestao.dtos.fornecedor.FornecedorCadastroRequest;
import com.gestao.dtos.fornecedor.FornecedorResponse;
import com.gestao.dtos.fornecedor.FornecedorUpdateRequest;
import com.gestao.entities.Fornecedor;
import com.gestao.entities.Perfil;
import org.springframework.stereotype.Component;

@Component
public class FornecedorMapper {

    public Fornecedor toEntity(FornecedorCadastroRequest request) {
        Fornecedor fornecedor = new Fornecedor();

        fornecedor.setNomeCompleto(request.nomeCompleto());
        fornecedor.setCnpj(request.cnpj());
        fornecedor.setEmail(request.email());
        fornecedor.setSenha(request.senha());

        Perfil perfil = new Perfil();
        perfil.setNome("FORNECEDOR");
        fornecedor.setPerfil(perfil);

        return fornecedor;
    }

    public FornecedorResponse toResponse(Fornecedor fornecedor) {
        if (fornecedor == null) return null;

        return new FornecedorResponse(
                fornecedor.getId(),
                fornecedor.getNomeCompleto(),
                fornecedor.getCnpj(),
                fornecedor.getEmail(),
                fornecedor.getPerfil() != null ? fornecedor.getPerfil().getNome() : "FORNECEDOR"
        );
    }

    public void updateEntity(Fornecedor fornecedor, FornecedorUpdateRequest request) {
        // CORREÇÃO 3: Acesso sem o prefixo "get"
        if (request.nomeCompleto() != null) {
            fornecedor.setNomeCompleto(request.nomeCompleto());
        }
        // Adicione outros campos se necessário
    }
}