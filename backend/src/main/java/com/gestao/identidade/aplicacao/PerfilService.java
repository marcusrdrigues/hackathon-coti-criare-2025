package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.PerfilRepositorio;
import com.gestao.identidade.dominio.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepositorio perfilRepositorio;

    @Transactional(readOnly = true)
    public Perfil buscarPorNome(String nome) {
        return perfilRepositorio.buscarPorNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil não encontrado!"));
    }
}
