package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.PerfilRepositorio;
import com.gestao.identidade.dominio.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepositorio perfilRepositorio;

    @Transactional
    public Perfil criarPerfil(Perfil perfil) {
        if (perfilRepositorio.existeComNome(perfil.getNome())) {
            throw new RecursoDuplicadoException("Perfil já existe com esse nome!");
        }
        return perfilRepositorio.salvar(perfil);
    }

    @Transactional(readOnly = true)
    public Perfil buscarPorNome(String nome) {
        return perfilRepositorio.buscarPorNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado!"));
    }

    @Transactional(readOnly = true)
    public Perfil buscarPorId(UUID id) {
        return perfilRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado!"));
    }

    @Transactional(readOnly = true)
    public List<Perfil> listarTodos() {
        return perfilRepositorio.listarTodos();
    }
}
