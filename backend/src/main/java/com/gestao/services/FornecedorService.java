package com.gestao.services;

import com.gestao.entities.Fornecedor;
import com.gestao.entities.Perfil;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.FornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private FornecedorRepository fornecedorRepository;
    private PerfilService perfilService;

    // Cadastrar fornecedor
    public Fornecedor cadastrarFornecedor(Fornecedor fornecedor) {
        // Validar se email já existe
        if (fornecedorRepository.existsByEmail(fornecedor.getEmail())) {
            throw new DuplicateResourceException("Email já cadastrado!");
        }

        // Validar se CNPJ já existe
        if (fornecedorRepository.existsByCnpj(fornecedor.getCnpj())) {
            throw new DuplicateResourceException("CNPJ já cadastrado!");
        }

        // Buscar perfil "FORNECEDOR"
        Perfil perfilFornecedor = perfilService.buscarPorNome("FORNECEDOR");
        fornecedor.setPerfil(perfilFornecedor);

        // TODO: Criptografar senha (implementar depois)

        return fornecedorRepository.save(fornecedor);
    }

    // Buscar fornecedor por ID
    public Fornecedor buscarPorId(UUID id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado!"));
    }

    // Buscar fornecedor por email
    public Fornecedor buscarPorEmail(String email) {
        return fornecedorRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrada!"));
    }

    // Atualizar fornecedor
    public Fornecedor atualizarFornecedor(UUID id, Fornecedor fornecedorAtualizado) {
        Fornecedor fornecedor = buscarPorId(id);
        fornecedor.setNomeCompleto(fornecedorAtualizado.getNomeCompleto());
        return fornecedorRepository.save(fornecedor);
    }

    // Listar todos os fornecedores
    public List<Fornecedor> listarTodos() {
        return fornecedorRepository.findAll();
    }

    // Deletar fornecedor
    public void deletarFornecedor(UUID id) {
        Fornecedor fornecedor = buscarPorId(id);
        fornecedorRepository.delete(fornecedor);
    }
}