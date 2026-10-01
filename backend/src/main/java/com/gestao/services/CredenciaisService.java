package com.gestao.services;

import com.gestao.exceptions.BusinessException;
import com.gestao.exceptions.DuplicateResourceException;
import com.gestao.repositories.EmpresaRepository;
import com.gestao.repositories.FornecedorRepository;
import com.gestao.utils.Documentos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Regras compartilhadas entre o cadastro de empresas e de fornecedores:
 * e-mail único na plataforma inteira, CNPJ válido e senha com hash.
 */
@Service
@RequiredArgsConstructor
public class CredenciaisService {

    private final EmpresaRepository empresaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * O login procura o e-mail nas duas tabelas, então o mesmo e-mail não pode
     * existir como empresa e como fornecedor ao mesmo tempo.
     */
    public void validarEmailDisponivel(String email) {
        if (empresaRepository.existsByEmail(email) || fornecedorRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email já cadastrado!");
        }
    }

    /** Valida os dígitos verificadores e devolve o CNPJ só com números. */
    public String normalizarCnpj(String cnpj) {
        if (!Documentos.cnpjValido(cnpj)) {
            throw new BusinessException("CNPJ inválido!");
        }
        return Documentos.somenteDigitos(cnpj);
    }

    public String gerarHash(String senha) {
        return passwordEncoder.encode(senha);
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
            return passwordEncoder.matches(senhaDigitada, senhaGravada);
        }
        return senhaGravada.equals(senhaDigitada);
    }

    public boolean ehHashBcrypt(String senha) {
        return senha != null && senha.startsWith("$2");
    }
}
