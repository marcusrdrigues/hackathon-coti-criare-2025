package com.gestao.services;

import com.gestao.entities.Fornecedor;
import com.gestao.entities.Perfil;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.FornecedorRepository;
import com.gestao.utils.Documentos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final PerfilService perfilService;
    private final CredenciaisService credenciaisService;

    @Transactional
    public Fornecedor cadastrarFornecedor(Fornecedor fornecedor) {
        fornecedor.setEmail(Documentos.normalizarEmail(fornecedor.getEmail()));
        fornecedor.setCnpj(credenciaisService.normalizarCnpj(fornecedor.getCnpj()));
        fornecedor.setNomeCompleto(fornecedor.getNomeCompleto().trim());

        credenciaisService.validarEmailDisponivel(fornecedor.getEmail());

        if (fornecedorRepository.existsByCnpj(fornecedor.getCnpj())) {
            throw new DuplicateResourceException("CNPJ já cadastrado!");
        }

        Perfil perfilFornecedor = perfilService.buscarPorNome("FORNECEDOR");
        fornecedor.setPerfil(perfilFornecedor);
        fornecedor.setSenha(credenciaisService.gerarHash(fornecedor.getSenha()));

        return fornecedorRepository.save(fornecedor);
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(UUID id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado!"));
    }

    @Transactional(readOnly = true)
    public Fornecedor buscarPorEmail(String email) {
        return fornecedorRepository.findByEmail(Documentos.normalizarEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado!"));
    }

    @Transactional
    public Fornecedor atualizarFornecedor(UUID id, String nomeCompleto) {
        Fornecedor fornecedor = buscarPorId(id);
        if (nomeCompleto != null && !nomeCompleto.isBlank()) {
            fornecedor.setNomeCompleto(nomeCompleto.trim());
        }
        return fornecedorRepository.save(fornecedor);
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos() {
        return fornecedorRepository.findAll();
    }

    @Transactional
    public void deletarFornecedor(UUID id) {
        Fornecedor fornecedor = buscarPorId(id);
        fornecedorRepository.delete(fornecedor);
    }
}
