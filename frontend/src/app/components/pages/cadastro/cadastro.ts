import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { TipoOrganizacao } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { CadastroService } from '../../../core/services/cadastro.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { cnpjValido, mascararCnpj } from '../../../core/utils/formatos';
import { LayoutAcesso } from '../../shared/layout-acesso/layout-acesso';

type Campo = 'razaoSocial' | 'cnpj' | 'nome' | 'email' | 'senha';

const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-cadastro',
  imports: [FormsModule, RouterLink, LayoutAcesso],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css',
})
export class CadastroComponent {
  private readonly cadastroService = inject(CadastroService);
  private readonly auth = inject(AuthService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  protected readonly tipo = signal<TipoOrganizacao>('EMPRESA');
  protected readonly razaoSocial = signal('');
  protected readonly cnpj = signal('');
  /** O nome da pessoa que cria a conta (ela será a proprietária da organização) */
  protected readonly nome = signal('');
  protected readonly email = signal('');
  protected readonly senha = signal('');

  protected readonly carregando = signal(false);
  protected readonly erroGeral = signal<string | null>(null);
  /** Erros que vieram da API para um campo específico (ex.: e-mail já cadastrado). */
  protected readonly errosServidor = signal<Partial<Record<Campo, string>>>({});
  /** Campos que a pessoa já visitou: só então mostramos o erro, para não acusar antes da hora. */
  protected readonly tocados = signal<Set<Campo>>(new Set());

  protected readonly senhaTemTamanho = computed(() => this.senha().length >= 8);
  protected readonly senhaTemLetraENumero = computed(
    () => /[A-Za-z]/.test(this.senha()) && /\d/.test(this.senha()),
  );

  /** Validação no navegador, com a mesma regra da API. */
  protected readonly erros = computed<Partial<Record<Campo, string>>>(() => {
    const erros: Partial<Record<Campo, string>> = {};
    if (!this.razaoSocial().trim()) {
      erros.razaoSocial = 'Informe a razão social.';
    }
    if (this.cnpj().replace(/\D/g, '').length !== 14) {
      erros.cnpj = 'Informe os 14 dígitos do CNPJ.';
    } else if (!cnpjValido(this.cnpj())) {
      erros.cnpj = 'Esse CNPJ não é válido. Confira os números.';
    }
    if (!this.nome().trim()) {
      erros.nome = 'Informe o seu nome.';
    }
    if (!EMAIL_VALIDO.test(this.email().trim())) {
      erros.email = 'Informe um e-mail no formato nome@empresa.com.br.';
    }
    if (!this.senhaTemTamanho() || !this.senhaTemLetraENumero()) {
      erros.senha = 'Use pelo menos 8 caracteres, com letras e números.';
    }
    return { ...erros, ...this.errosServidor() };
  });

  protected readonly textoDoTipo = computed(() =>
    this.tipo() === 'EMPRESA'
      ? 'Publique o que precisa comprar e negocie com fornecedores.'
      : 'Encontre cotações abertas e envie suas propostas.',
  );

  erroVisivel(campo: Campo): string | null {
    return this.tocados().has(campo) ? (this.erros()[campo] ?? null) : null;
  }

  tocar(campo: Campo): void {
    this.tocados.update((atual) => new Set(atual).add(campo));
  }

  alterar(campo: Campo, valor: string): void {
    const sinal = {
      razaoSocial: this.razaoSocial,
      cnpj: this.cnpj,
      nome: this.nome,
      email: this.email,
      senha: this.senha,
    }[campo];
    sinal.set(campo === 'cnpj' ? mascararCnpj(valor) : valor);
    // A pessoa corrigiu o campo: o erro que veio da API deixa de valer
    this.errosServidor.update(({ [campo]: _removido, ...resto }) => resto);
  }

  cadastrar(): void {
    this.tocados.set(new Set<Campo>(['razaoSocial', 'cnpj', 'nome', 'email', 'senha']));
    if (Object.keys(this.erros()).length > 0) {
      return;
    }

    const email = this.email().trim();
    const senha = this.senha();

    this.carregando.set(true);
    this.erroGeral.set(null);

    // A organização e a pessoa proprietária nascem juntas; depois, já entra direto no portal
    this.cadastroService
      .cadastrar({
        tipo: this.tipo(),
        razaoSocial: this.razaoSocial().trim(),
        cnpj: this.cnpj(),
        nome: this.nome().trim(),
        email,
        senha,
      })
      .pipe(switchMap(() => this.auth.login({ email, senha })))
      .subscribe({
        next: () => {
          this.notificacao.sucesso('Conta criada. Bem-vindo ao portal.');
          this.router.navigateByUrl(this.auth.rotaInicial());
        },
        error: (e) => {
          this.carregando.set(false);
          this.mostrarErroDoServidor(mensagemDeErro(e));
        },
      });
  }

  /** Coloca o erro da API ao lado do campo a que ele se refere, quando dá para saber qual é. */
  private mostrarErroDoServidor(mensagem: string): void {
    const texto = mensagem.toLowerCase();
    if (texto.includes('email já cadastrado')) {
      this.errosServidor.set({ email: 'Já existe uma conta com esse e-mail. Entre ou use outro e-mail.' });
    } else if (texto.includes('cnpj já cadastrado')) {
      this.errosServidor.set({
        cnpj: `Esse CNPJ já está cadastrado como ${this.tipo() === 'EMPRESA' ? 'empresa' : 'fornecedor'}.`,
      });
    } else if (texto.includes('cnpj inválido')) {
      this.errosServidor.set({ cnpj: 'Esse CNPJ não é válido. Confira os números.' });
    } else {
      this.erroGeral.set(mensagem);
    }
  }
}
