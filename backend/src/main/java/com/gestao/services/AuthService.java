package com.gestao.services;

import com.gestao.dtos.auth.LoginResponse;
import com.gestao.entities.Empresa;
import com.gestao.entities.Fornecedor;
import com.gestao.exceptions.UnauthorizedException;
import com.gestao.repositories.EmpresaRepository;
import com.gestao.repositories.FornecedorRepository;
import com.gestao.utils.Documentos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String MENSAGEM_ERRO = "Email ou senha inválidos.";

    private final EmpresaRepository empresaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final CredenciaisService credenciaisService;

    /**
     * Procura o e-mail entre empresas e fornecedores e devolve quem é o usuário.
     * A mensagem de erro é a mesma para e-mail inexistente e senha errada,
     * para não revelar quais e-mails estão cadastrados.
     */
    @Transactional
    public LoginResponse login(String email, String senha) {
        String emailNormalizado = Documentos.normalizarEmail(email);

        Optional<Empresa> empresa = empresaRepository.findByEmail(emailNormalizado);
        if (empresa.isPresent()) {
            Empresa e = empresa.get();
            validarSenha(senha, e.getSenha());
            if (!credenciaisService.ehHashBcrypt(e.getSenha())) {
                // Cadastro antigo com senha em texto puro: aproveita o login para gravar o hash
                e.setSenha(credenciaisService.gerarHash(senha));
            }
            return new LoginResponse(e.getId(), e.getRazaoSocial(), e.getEmail(), e.getCnpj(), "EMPRESA");
        }

        Optional<Fornecedor> fornecedor = fornecedorRepository.findByEmail(emailNormalizado);
        if (fornecedor.isPresent()) {
            Fornecedor f = fornecedor.get();
            validarSenha(senha, f.getSenha());
            if (!credenciaisService.ehHashBcrypt(f.getSenha())) {
                f.setSenha(credenciaisService.gerarHash(senha));
            }
            return new LoginResponse(f.getId(), f.getNomeCompleto(), f.getEmail(), f.getCnpj(), "FORNECEDOR");
        }

        throw new UnauthorizedException(MENSAGEM_ERRO);
    }

    private void validarSenha(String senhaDigitada, String senhaGravada) {
        if (!credenciaisService.senhaConfere(senhaDigitada, senhaGravada)) {
            throw new UnauthorizedException(MENSAGEM_ERRO);
        }
    }
}
