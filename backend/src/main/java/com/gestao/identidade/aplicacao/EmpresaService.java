package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.EmpresaRepositorio;
import com.gestao.identidade.dominio.Empresa;
import com.gestao.identidade.dominio.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepositorio empresaRepositorio;
    private final PerfilService perfilService;
    private final CredenciaisService credenciaisService;

    @Transactional
    public Empresa cadastrarEmpresa(Empresa empresa) {
        empresa.setEmail(Documentos.normalizarEmail(empresa.getEmail()));
        empresa.setCnpj(credenciaisService.normalizarCnpj(empresa.getCnpj()));
        empresa.setRazaoSocial(empresa.getRazaoSocial().trim());

        credenciaisService.validarEmailDisponivel(empresa.getEmail());

        if (empresaRepositorio.existeComCnpj(empresa.getCnpj())) {
            throw new RecursoDuplicadoException("CNPJ já cadastrado!");
        }

        Perfil perfilEmpresa = perfilService.buscarPorNome("EMPRESA");
        empresa.setPerfil(perfilEmpresa);
        empresa.setSenha(credenciaisService.gerarHash(empresa.getSenha()));

        return empresaRepositorio.salvar(empresa);
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorId(UUID id) {
        return empresaRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada!"));
    }

    @Transactional(readOnly = true)
    public Empresa buscarPorEmail(String email) {
        return empresaRepositorio.buscarPorEmail(Documentos.normalizarEmail(email))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa não encontrada!"));
    }

    @Transactional
    public Empresa atualizarEmpresa(UUID id, String razaoSocial) {
        Empresa empresa = buscarPorId(id);
        if (razaoSocial != null && !razaoSocial.isBlank()) {
            empresa.setRazaoSocial(razaoSocial.trim());
        }
        return empresaRepositorio.salvar(empresa);
    }

    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return empresaRepositorio.listarTodos();
    }

    @Transactional
    public void deletarEmpresa(UUID id) {
        Empresa empresa = buscarPorId(id);
        empresaRepositorio.excluir(empresa);
    }
}
