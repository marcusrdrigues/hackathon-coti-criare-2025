package com.gestao.services;

import com.gestao.entities.Empresa;
import com.gestao.entities.Perfil;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private EmpresaRepository empresaRepository;
    private PerfilService perfilService;

    // Cadastrar empresa
    public Empresa cadastrarEmpresa(Empresa empresa) {
        // Validar se email já existe
        if (empresaRepository.existsByEmail(empresa.getEmail())) {
            throw new DuplicateResourceException("Email já cadastrado!");
        }

        // Validar se CNPJ já existe
        if (empresaRepository.existsByCnpj(empresa.getCnpj())) {
            throw new DuplicateResourceException("CNPJ já cadastrado!");
        }

        // Buscar perfil "EMPRESA"
        Perfil perfilEmpresa = perfilService.buscarPorNome("EMPRESA");
        empresa.setPerfil(perfilEmpresa);

        // TODO: Criptografar senha (implementar depois)

        return empresaRepository.save(empresa);
    }

    // Buscar empresa por ID
    public Empresa buscarPorId(UUID id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada!"));
    }

    // Buscar empresa por email
    public Empresa buscarPorEmail(String email) {
        return empresaRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada!"));
    }

    // Atualizar empresa
    public Empresa atualizarEmpresa(UUID id, Empresa empresaAtualizada) {
        Empresa empresa = buscarPorId(id);
        empresa.setRazaoSocial(empresaAtualizada.getRazaoSocial());
        return empresaRepository.save(empresa);
    }

    // Listar todas as empresas
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    // Deletar empresa
    public void deletarEmpresa(UUID id) {
        Empresa empresa = buscarPorId(id);
        empresaRepository.delete(empresa);
    }
}