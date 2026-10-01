import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { mensagemDeErro } from '../../../core/utils/erros';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  credenciais = { email: '', senha: '' };
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);

  fazerLogin(): void {
    if (!this.credenciais.email || !this.credenciais.senha) {
      this.erro.set('Preencha e-mail e senha.');
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.auth.login(this.credenciais).subscribe({
      next: () => this.router.navigateByUrl(this.auth.rotaInicial()),
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      },
    });
  }
}
