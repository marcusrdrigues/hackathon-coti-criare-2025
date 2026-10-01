package com.gestao.services;

import com.gestao.entities.Empresa;
import com.gestao.entities.Perfil;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.exceptions.ResourceNotFoundException;
import com.gestao.repositories.EmpresaRepository;
import com.gestao.utils.Documentos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PerfilService perfilService;
    private final CredenciaisService credenciaisService;

    @Transactional
    public Empresa cadastrarEmpresa(Empresa empresa) {
        empresa.setEmail(Documentos.normalizarEmail(empresa.getEmail()));
        empresa.setCnpj(credenciaisService.normalizarCnpj(empresa.getCnpj()));
        empresa.setRazaoSocial(empresa.getRazaoSocial().trim());

        credenciaisService.validarEmailDisponivel(empresa.getEmail());

        if (empresaRepository.existsByCnpj(empresa.getCnpj())) {
            throw new DuplicateResourceException("CNPJ já cadastrado!");
        }

        Perfil perfilEmpresa = perfilService.buscarPorNome("EMPRESA");
        empresa.setPerfil(perfilEmpresa);
        empresa.setSenha(credenciaisService.gerarHash(empresa.getSenha()));

        return empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorId(UUID id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada!"));
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorEmail(String email) {
        return empresaRepository.findByEmail(Documentos.normalizarEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada!"));
    }

    @Transactional
    public Empresa atualizarEmpresa(UUID id, String razaoSocial) {
        Empresa empresa = buscarPorId(id);
        if (razaoSocial != null && !razaoSocial.isBlank()) {
            empresa.setRazaoSocial(razaoSocial.trim());
        }
        return empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    @Transactional
    public void deletarEmpresa(UUID id) {
        Empresa empresa = buscarPorId(id);
        empresaRepository.delete(empresa);
    }
}
