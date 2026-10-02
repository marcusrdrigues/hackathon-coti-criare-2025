package com.gestao.identidade.aplicacao;

import com.gestao.compartilhado.dominio.Documentos;
import com.gestao.compartilhado.dominio.RecursoDuplicadoException;
import com.gestao.compartilhado.dominio.RegraDeNegocioException;
import com.gestao.identidade.aplicacao.porta.CodificadorDeSenha;
import com.gestao.identidade.aplicacao.porta.EmpresaRepositorio;
import com.gestao.identidade.aplicacao.porta.FornecedorRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras compartilhadas entre o cadastro de empresas e de fornecedores:
 * e-mail único na plataforma inteira, CNPJ válido e senha com hash.
 */
@Service
@RequiredArgsConstructor
public class CredenciaisService {

    private final EmpresaRepositorio empresaRepositorio;
    private final FornecedorRepositorio fornecedorRepositorio;
    private final CodificadorDeSenha codificador;

    /**
     * O login procura o e-mail nas duas tabelas, então o mesmo e-mail não pode
     * existir como empresa e como fornecedor ao mesmo tempo.
     */
    public void validarEmailDisponivel(String email) {
        if (empresaRepositorio.existeComEmail(email) || fornecedorRepositorio.existeComEmail(email)) {
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

    /**
     * Compara a senha digitada com a gravada. Cadastros feitos antes da
     * criptografia têm a senha em texto puro; nesse caso a comparação é direta.
     */
    public boolean senhaConfere(String senhaDigitada, String senhaGravada) {
        if (senhaGravada == null) {
            return false;
        }
        if (ehHashBcrypt(senhaGravada)) {
            return codificador.confere(senhaDigitada, senhaGravada);
        }
        return senhaGravada.equals(senhaDigitada);
    }

    public boolean ehHashBcrypt(String senha) {
        return senha != null && senha.startsWith("$2");
    }
}
