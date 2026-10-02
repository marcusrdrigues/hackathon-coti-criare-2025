import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConviteAberto } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { EquipeService } from '../../../core/services/equipe.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { LayoutAcesso } from '../../shared/layout-acesso/layout-acesso';

/**
 * Quem recebeu o link de convite chega aqui. O token vem depois do "#" (nunca vai ao
 * servidor na URL) e sai da barra de endereço assim que é lido.
 */
@Component({
  selector: 'app-convite',
  imports: [FormsModule, RouterLink, LayoutAcesso],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './convite.html',
})
export class ConviteComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly equipeService = inject(EquipeService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly router = inject(Router);

  private token = '';

  protected readonly convite = signal<ConviteAberto | null>(null);
  protected readonly invalido = signal<string | null>(null);
  protected readonly nome = signal('');
  protected readonly senha = signal('');
  protected readonly tentouEnviar = signal(false);
  protected readonly carregando = signal(false);
  protected readonly erroGeral = signal<string | null>(null);

  protected readonly senhaTemTamanho = computed(() => this.senha().length >= 8);
  protected readonly senhaTemLetraENumero = computed(
    () => /[A-Za-z]/.test(this.senha()) && /\d/.test(this.senha()),
  );
  protected readonly erros = computed(() => ({
    nome: this.nome().trim() ? null : 'Informe o seu nome.',
    senha:
      this.senhaTemTamanho() && this.senhaTemLetraENumero()
        ? null
        : 'Use pelo menos 8 caracteres, com letras e números.',
  }));
  protected readonly tipo = computed(() =>
    this.convite()?.tipo === 'EMPRESA' ? 'empresa' : 'fornecedor',
  );

  ngOnInit(): void {
    this.token = decodeURIComponent(globalThis.location?.hash?.slice(1) ?? '');
    // O token já foi lido: tira da barra de endereço e do histórico
    if (this.token) {
      history.replaceState(history.state, '', globalThis.location.pathname);
    }
    if (!this.token) {
      this.invalido.set(
        'Este link de convite está incompleto. Abra o link exatamente como você o recebeu.',
      );
      return;
    }
    this.equipeService.consultar(this.token).subscribe({
      next: (convite) => {
        this.convite.set(convite);
        this.nome.set(convite.nome);
      },
      error: (e) => this.invalido.set(mensagemDeErro(e)),
    });
  }

  aceitar(): void {
    this.tentouEnviar.set(true);
    if (this.erros().nome || this.erros().senha) {
      return;
    }
    this.carregando.set(true);
    this.erroGeral.set(null);
    this.equipeService.aceitar(this.token, this.nome().trim(), this.senha()).subscribe({
      next: (usuario) => {
        this.notificacao.sucesso(`Você entrou na equipe da ${usuario.organizacao.razaoSocial}.`);
        this.router.navigateByUrl(this.auth.rotaInicial());
      },
      error: (e) => {
        this.carregando.set(false);
        const mensagem = mensagemDeErro(e);
        if (mensagem.toLowerCase().includes('email já cadastrado')) {
          this.erroGeral.set(
            'Este e-mail já tem uma conta no portal. Entre com ele ou peça um convite para outro e-mail.',
          );
        } else {
          this.erroGeral.set(mensagem);
        }
      },
    });
  }
}
