package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.identidade.aplicacao.porta.MembroRepositorio;
import com.gestao.identidade.aplicacao.porta.OrganizacaoRepositorio;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Organizacao;
import com.gestao.identidade.dominio.Papel;
import com.gestao.identidade.dominio.TipoOrganizacao;
import com.gestao.identidade.dominio.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Entrada de organizações e pessoas na plataforma. */
@Service
@RequiredArgsConstructor
public class CadastroService {

    private final OrganizacaoRepositorio organizacaoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final MembroRepositorio membroRepositorio;
    private final CredenciaisService credenciais;

    /** Dados do cadastro público: a organização e quem a cadastra. */
    public record NovaOrganizacao(TipoOrganizacao tipo, String razaoSocial, String cnpj,
                                  String nome, String email, String senha) {}

    /**
     * Cria a organização e a pessoa proprietária juntas: ou as duas, ou nenhuma.
     * O CNPJ é único por tipo (a mesma empresa pode comprar e fornecer); o e-mail, na plataforma toda.
     */
    @Transactional
    public UsuarioAutenticado cadastrar(NovaOrganizacao dados) {
        String cnpj = credenciais.normalizarCnpj(dados.cnpj());
        if (organizacaoRepositorio.existeComCnpj(cnpj, dados.tipo())) {
            throw new RecursoDuplicadoException("CNPJ já cadastrado!");
        }
        Usuario usuario = novaPessoa(dados.nome(), dados.email(), dados.senha());
        Organizacao organizacao = organizacaoRepositorio.salvar(
                new Organizacao(dados.tipo(), dados.razaoSocial().trim(), cnpj));
        return UsuarioAutenticado.de(membroRepositorio.salvar(new Membro(usuario, organizacao, Papel.PROPRIETARIO)));
    }

    /** Põe uma pessoa nova numa organização que já existe, como membro. */
    @Transactional
    public UsuarioAutenticado adicionarMembro(UUID organizacaoId, String nome, String email, String senha) {
        Organizacao organizacao = organizacaoRepositorio.buscarPorId(organizacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Organização inexistente: " + organizacaoId));
        Usuario usuario = novaPessoa(nome, email, senha);
        return UsuarioAutenticado.de(membroRepositorio.salvar(new Membro(usuario, organizacao, Papel.MEMBRO)));
    }

    private Usuario novaPessoa(String nome, String email, String senha) {
        String emailNormalizado = Documentos.normalizarEmail(email);
        credenciais.validarEmailDisponivel(emailNormalizado);
        return usuarioRepositorio.salvar(new Usuario(nome.trim(), emailNormalizado, credenciais.gerarHash(senha)));
    }
}
