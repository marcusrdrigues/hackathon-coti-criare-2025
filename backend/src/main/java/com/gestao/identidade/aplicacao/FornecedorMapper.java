package com.gestao.identidade.aplicacao;

import com.gestao.identidade.aplicacao.dto.FornecedorCadastroRequest;
import com.gestao.identidade.aplicacao.dto.FornecedorResponse;
import com.gestao.identidade.dominio.Fornecedor;
import org.springframework.stereotype.Component;

@Component
public class FornecedorMapper {

    public Fornecedor toEntity(FornecedorCadastroRequest request) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setNomeCompleto(request.nomeCompleto());
        fornecedor.setCnpj(request.cnpj());
        fornecedor.setEmail(request.email());
        fornecedor.setSenha(request.senha());
        // O perfil é definido no service, buscando o registro "FORNECEDOR" no banco
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
}
