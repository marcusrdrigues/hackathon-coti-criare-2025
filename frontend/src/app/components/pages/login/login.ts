import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ContaDemo, TipoUsuario, Usuario } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { mensagemDeErro } from '../../../core/utils/erros';

/** Depois de quanto tempo avisar que o servidor gratuito pode estar "acordando". */
const AVISO_SERVIDOR_LENTO_MS = 4000;

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  credenciais = { email: '', senha: '' };
  protected readonly carregando = signal(false);
  protected readonly entrandoComo = signal<TipoUsuario | null>(null);
  protected readonly servidorLento = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly contasDemo = signal<ContaDemo[]>([]);

  ngOnInit(): void {
    this.auth.contasDemo().subscribe((contas) => this.contasDemo.set(contas));
  }

  fazerLogin(): void {
    if (!this.credenciais.email || !this.credenciais.senha) {
      this.erro.set('Preencha e-mail e senha.');
      return;
    }
    this.entrar(this.auth.login(this.credenciais));
  }

  entrarDemo(perfil: TipoUsuario): void {
    this.entrandoComo.set(perfil);
    this.entrar(this.auth.entrarDemo(perfil));
  }

  private entrar(login$: Observable<Usuario>): void {
    this.carregando.set(true);
    this.erro.set(null);

    // Em hospedagem gratuita, a primeira requisição depois de um tempo parado pode demorar
    const aviso = setTimeout(() => this.servidorLento.set(true), AVISO_SERVIDOR_LENTO_MS);
    this.destroyRef.onDestroy(() => clearTimeout(aviso));
    const finalizar = () => {
      clearTimeout(aviso);
      this.servidorLento.set(false);
    };

    login$.subscribe({
      next: () => {
        finalizar();
        this.router.navigateByUrl(this.auth.rotaInicial());
      },
      error: (e) => {
        finalizar();
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
        this.entrandoComo.set(null);
      },
    });
  }
}
