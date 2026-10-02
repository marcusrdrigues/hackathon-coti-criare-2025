package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.RecursoNaoEncontradoException;
import com.gestao.identidade.aplicacao.porta.OrganizacaoRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consulta de organizações e pessoas para os outros módulos (quem é dono e quem fez cada coisa). */
@Service
@RequiredArgsConstructor
public class OrganizacaoService {

    private final OrganizacaoRepositorio organizacaoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    @Transactional(readOnly = true)
    public Organizacao buscarPorId(UUID id) {
        return organizacaoRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada!"));
    }

    @Transactional(readOnly = true)
    public Usuario buscarUsuario(UUID id) {
        return usuarioRepositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado!"));
    }
}
