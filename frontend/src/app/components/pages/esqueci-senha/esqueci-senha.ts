import { ChangeDetectionStrategy, Component, OnInit, computed, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RedefinicaoSenhaService } from '../../../core/services/redefinicao-senha.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { LayoutAcesso } from '../../shared/layout-acesso/layout-acesso';

/** Formato mínimo: o servidor confere de novo. */
const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Pede o link de redefinição (spec 004, R1). A confirmação é a mesma com conta ou sem:
 * a tela nunca diz se o e-mail tem cadastro.
 */
@Component({
  selector: 'app-esqueci-senha',
  imports: [FormsModule, RouterLink, LayoutAcesso],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './esqueci-senha.html',
})
export class EsqueciSenha implements OnInit {
  private readonly redefinicao = inject(RedefinicaoSenhaService);

  protected readonly email = signal('');
  protected readonly tentouEnviar = signal(false);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  /** O e-mail para o qual o pedido foi feito; preenchido, a tela mostra a confirmação. */
  protected readonly enviadoPara = signal<string | null>(null);

  protected readonly erroEmail = computed(() => {
    const email = this.email().trim();
    if (!email) {
      return 'Preencha o seu e-mail.';
    }
    return FORMATO_EMAIL.test(email) ? null : 'Confira o e-mail: falta alguma parte.';
  });

  ngOnInit(): void {
    // Vindo do login, o e-mail que a pessoa já tinha digitado chega junto
    const digitado = (history.state as { email?: unknown } | null)?.email;
    if (typeof digitado === 'string') {
      this.email.set(digitado);
    }
  }

  enviar(): void {
    this.tentouEnviar.set(true);
    if (this.erroEmail()) {
      return;
    }
    const email = this.email().trim();
    this.carregando.set(true);
    this.erro.set(null);
    this.redefinicao.pedir(email).subscribe({
      next: () => {
        this.carregando.set(false);
        this.enviadoPara.set(email);
      },
      error: (e) => {
        this.carregando.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  outroEmail(): void {
    this.enviadoPara.set(null);
    this.tentouEnviar.set(false);
    this.email.set('');
  }
}
