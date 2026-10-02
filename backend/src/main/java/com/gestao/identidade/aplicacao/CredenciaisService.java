package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.identidade.aplicacao.porta.CodificadorDeSenha;
import com.gestao.identidade.aplicacao.porta.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Regras de credenciais: e-mail único na plataforma, CNPJ válido e senha guardada só como hash. */
@Service
@RequiredArgsConstructor
public class CredenciaisService {

    private final UsuarioRepositorio usuarioRepositorio;
    private final CodificadorDeSenha codificador;

    public void validarEmailDisponivel(String emailNormalizado) {
        if (usuarioRepositorio.existeComEmail(emailNormalizado)) {
            throw new RecursoDuplicadoException("Email já cadastrado!");
        }
    }

    /** Valida os dígitos verificadores e devolve o CNPJ só com números. */
    public String normalizarCnpj(String cnpj) {
        if (!Documentos.cnpjValido(cnpj)) {
            throw new RegraDeNegocioException("CNPJ inválido!");
        }
        return Documentos.somenteDigitos(cnpj);
    }

    public String gerarHash(String senha) {
        return codificador.codificar(senha);
    }

    public boolean senhaConfere(String senhaDigitada, String hashGravado) {
        return hashGravado != null && senhaDigitada != null && codificador.confere(senhaDigitada, hashGravado);
    }
}
