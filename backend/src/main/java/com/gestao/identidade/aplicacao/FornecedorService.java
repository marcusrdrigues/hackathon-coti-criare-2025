package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.FornecedorRepositorio;
import com.gestao.identidade.dominio.Fornecedor;
import com.gestao.identidade.dominio.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepositorio fornecedorRepositorio;
    private final PerfilService perfilService;
    private final CredenciaisService credenciaisService;

    @Transactional
    public Fornecedor cadastrarFornecedor(Fornecedor fornecedor) {
        fornecedor.setEmail(Documentos.normalizarEmail(fornecedor.getEmail()));
        fornecedor.setCnpj(credenciaisService.normalizarCnpj(fornecedor.getCnpj()));
        fornecedor.setNomeCompleto(fornecedor.getNomeCompleto().trim());

        credenciaisService.validarEmailDisponivel(fornecedor.getEmail());

        if (fornecedorRepositorio.existeComCnpj(fornecedor.getCnpj())) {
            throw new RecursoDuplicadoException("CNPJ já cadastrado!");
        }

        Perfil perfilFornecedor = perfilService.buscarPorNome("FORNECEDOR");
        fornecedor.setPerfil(perfilFornecedor);
        fornecedor.setSenha(credenciaisService.gerarHash(fornecedor.getSenha()));

        return fornecedorRepositorio.salvar(fornecedor);
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(UUID id) {
        return fornecedorRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado!"));
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorEmail(String email) {
        return fornecedorRepositorio.buscarPorEmail(Documentos.normalizarEmail(email))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado!"));
    }

    @Transactional
    public Fornecedor atualizarFornecedor(UUID id, String nomeCompleto) {
        Fornecedor fornecedor = buscarPorId(id);
        if (nomeCompleto != null && !nomeCompleto.isBlank()) {
            fornecedor.setNomeCompleto(nomeCompleto.trim());
        }
        return fornecedorRepositorio.salvar(fornecedor);
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos() {
        return fornecedorRepositorio.listarTodos();
    }

    @Transactional
    public void deletarFornecedor(UUID id) {
        Fornecedor fornecedor = buscarPorId(id);
        fornecedorRepositorio.excluir(fornecedor);
    }
}
