import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RedefinicaoSenhaService } from '../../../core/services/redefinicao-senha.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { LayoutAcesso } from '../../shared/layout-acesso/layout-acesso';

const LINK_INVALIDO = 'Este link não vale mais. Peça um link novo.';

/**
 * Quem abriu o link do e-mail chega aqui (spec 004, R2 e R3). O token vem no fragmento
 * ({@code #token=}), que o navegador nunca envia a um servidor, e sai da barra de endereço
 * assim que é lido.
 */
@Component({
  selector: 'app-redefinir-senha',
  imports: [FormsModule, RouterLink, LayoutAcesso],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './redefinir-senha.html',
})
export class RedefinirSenha implements OnInit {
  private readonly redefinicao = inject(RedefinicaoSenhaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private token = '';

  /** 'conferindo' enquanto pergunta ao servidor; 'valido' mostra o formulário. */
  protected readonly estado = signal<'conferindo' | 'valido' | 'invalido'>('conferindo');
  protected readonly motivo = signal(LINK_INVALIDO);
  protected readonly senha = signal('');
  protected readonly confirmacao = signal('');
  protected readonly tentouEnviar = signal(false);
  protected readonly carregando = signal(false);
  protected readonly erroGeral = signal<string | null>(null);

  protected readonly senhaTemTamanho = computed(() => this.senha().length >= 8);
  protected readonly senhaTemLetraENumero = computed(
    () => /[A-Za-z]/.test(this.senha()) && /\d/.test(this.senha()),
  );
  protected readonly erros = computed(() => ({
    senha:
      this.senhaTemTamanho() && this.senhaTemLetraENumero()
        ? null
        : 'Use pelo menos 8 caracteres, com letras e números.',
    confirmacao: this.confirmacao() === this.senha() ? null : 'As duas senhas não são iguais.',
  }));

  ngOnInit(): void {
    const fragmento = globalThis.location?.hash?.slice(1) ?? '';
    this.token = new URLSearchParams(fragmento).get('token')?.trim() ?? '';
    // O token já foi lido: sai da barra de endereço e do histórico
    if (fragmento) {
      history.replaceState(history.state, '', globalThis.location.pathname);
    }
    if (!this.token) {
      this.invalidar('Este link está incompleto. Abra o link exatamente como chegou no seu e-mail.');
      return;
    }
    this.redefinicao.consultar(this.token).subscribe({
      next: () => this.estado.set('valido'),
      error: (e) => this.invalidar(mensagemDeErro(e)),
    });
  }

  salvar(): void {
    this.tentouEnviar.set(true);
    if (this.erros().senha || this.erros().confirmacao) {
      return;
    }
    this.carregando.set(true);
    this.erroGeral.set(null);
    this.redefinicao.confirmar(this.token, this.senha()).subscribe({
      next: () => {
        // As sessões já caíram no servidor; a deste navegador também deixa de valer
        this.auth.esquecerSessao();
        this.router.navigate(['/pages/login'], { state: { senhaAlterada: true } });
      },
      error: (e: unknown) => {
        this.carregando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 404) {
          this.invalidar(mensagemDeErro(e));
        } else {
          this.erroGeral.set(mensagemDeErro(e));
        }
      },
    });
  }

  private invalidar(mensagem: string): void {
    this.motivo.set(mensagem);
    this.estado.set('invalido');
  }
}
