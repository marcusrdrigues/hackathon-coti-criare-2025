import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { TipoUsuario } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { CadastroService } from '../../../core/services/cadastro.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { mascararCnpj } from '../../../core/utils/formatos';

@Component({
  selector: 'app-cadastro',
  imports: [FormsModule, RouterLink],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css',
})
export class CadastroComponent {
  private readonly cadastroService = inject(CadastroService);
  private readonly auth = inject(AuthService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  registro = {
    nome: '',
    cnpj: '',
    email: '',
    senha: '',
    tipo: 'FORNECEDOR' as TipoUsuario,
  };

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);

  setTipo(tipo: TipoUsuario): void {
    this.registro.tipo = tipo;
  }

  aoDigitarCnpj(valor: string): void {
    this.registro.cnpj = mascararCnpj(valor);
  }

  cadastrar(): void {
    const { nome, cnpj, email, senha, tipo } = this.registro;

    if (!nome.trim() || !cnpj || !email.trim() || !senha) {
      this.erro.set('Preencha todos os campos.');
      return;
    }
    if (cnpj.replace(/\D/g, '').length !== 14) {
      this.erro.set('O CNPJ precisa ter 14 dígitos.');
      return;
    }
    if (senha.length < 6) {
      this.erro.set('A senha precisa ter pelo menos 6 caracteres.');
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    const cadastro$: Observable<unknown> =
      tipo === 'EMPRESA'
        ? this.cadastroService.cadastrarEmpresa({ razaoSocial: nome, cnpj, email, senha })
        : this.cadastroService.cadastrarFornecedor({ nomeCompleto: nome, cnpj, email, senha });

    // Depois do cadastro, já entra direto no portal
    cadastro$.pipe(switchMap(() => this.auth.login({ email, senha }))).subscribe({
      next: () => {
        this.notificacao.sucesso('Cadastro realizado! Bem-vindo ao portal.');
        this.router.navigateByUrl(this.auth.rotaInicial());
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      },
    });
  }
}
