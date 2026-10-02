import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Convite, Membro } from '../../../core/models';
import { AuthService } from '../../../core/services/auth.service';
import { EquipeService } from '../../../core/services/equipe.service';
import { NotificacaoService } from '../../../core/services/notificacao.service';
import { mensagemDeErro } from '../../../core/utils/erros';
import { iniciais } from '../../../core/utils/formatos';
import { ConfirmacaoService } from '../../../ui/confirmacao';
import { Icone } from '../../../ui/icone';
import { Painel } from '../../../ui/painel';

const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Quem faz parte da organização. Todos veem a equipe; o proprietário convida,
 * cancela convites e remove pessoas.
 */
@Component({
  selector: 'app-equipe',
  imports: [DatePipe, FormsModule, Icone, Painel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './equipe.html',
  styleUrl: './equipe.css',
})
export class Equipe implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly equipeService = inject(EquipeService);
  private readonly notificacao = inject(NotificacaoService);
  private readonly confirmacao = inject(ConfirmacaoService);

  protected readonly iniciais = iniciais;
  protected readonly proprietario = computed(() => this.auth.usuario()?.papel === 'PROPRIETARIO');

  protected readonly membros = signal<Membro[] | null>(null);
  protected readonly convites = signal<Convite[]>([]);
  protected readonly erro = signal<string | null>(null);

  // Painel de convite: primeiro o formulário, depois o link pronto para repassar
  protected readonly painelAberto = signal(false);
  protected readonly nome = signal('');
  protected readonly email = signal('');
  protected readonly tentouEnviar = signal(false);
  protected readonly enviando = signal(false);
  protected readonly link = signal<string | null>(null);
  protected readonly convidado = signal<Convite | null>(null);
  protected readonly copiado = signal(false);
  protected readonly podeCompartilhar =
    typeof navigator !== 'undefined' && typeof navigator.share === 'function';

  protected readonly erros = computed(() => ({
    nome: this.nome().trim() ? null : 'Informe o nome da pessoa.',
    email: EMAIL_VALIDO.test(this.email().trim())
      ? null
      : 'Informe um e-mail no formato nome@empresa.com.br.',
  }));

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.equipeService.membros().subscribe({
      next: (lista) => this.membros.set(lista),
      error: (e) => this.erro.set(mensagemDeErro(e)),
    });
    if (this.proprietario()) {
      this.equipeService.convites().subscribe({
        next: (lista) => this.convites.set(lista),
        error: (e) => this.notificacao.erro(mensagemDeErro(e)),
      });
    }
  }

  abrirConvite(): void {
    this.nome.set('');
    this.email.set('');
    this.tentouEnviar.set(false);
    this.link.set(null);
    this.convidado.set(null);
    this.copiado.set(false);
    this.painelAberto.set(true);
  }

  convidar(): void {
    this.tentouEnviar.set(true);
    const { nome, email } = this.erros();
    if (nome || email) {
      return;
    }
    this.enviando.set(true);
    this.equipeService
      .convidar({ nome: this.nome().trim(), email: this.email().trim() })
      .subscribe({
        next: (convite) => {
          this.enviando.set(false);
          this.convidado.set(convite);
          this.link.set(convite.token ? this.equipeService.link(convite.token) : null);
          this.carregar();
        },
        error: (e) => {
          this.enviando.set(false);
          this.notificacao.erro(this.mensagemDoConvite(mensagemDeErro(e)));
        },
      });
  }

  async copiar(): Promise<void> {
    const link = this.link();
    if (!link) {
      return;
    }
    try {
      await navigator.clipboard.writeText(link);
      this.copiado.set(true);
    } catch {
      this.notificacao.erro('Não foi possível copiar. Selecione o link e copie manualmente.');
    }
  }

  async compartilhar(): Promise<void> {
    const link = this.link();
    const convite = this.convidado();
    if (!link || !convite) {
      return;
    }
    try {
      await navigator.share({
        title: 'Convite para o Portal Criare',
        text: `${convite.nome}, este é o seu convite para a equipe da ${this.auth.usuario()?.organizacao?.razaoSocial}.`,
        url: link,
      });
    } catch {
      // Compartilhamento cancelado: nada a fazer
    }
  }

  async cancelarConvite(convite: Convite): Promise<void> {
    const confirmou = await this.confirmacao.confirmar({
      titulo: 'Cancelar o convite?',
      mensagem: `O link enviado para ${convite.nome} deixa de valer.`,
      confirmar: 'Cancelar convite',
      destrutivo: true,
    });
    if (!confirmou) {
      return;
    }
    this.equipeService.cancelarConvite(convite.id).subscribe({
      next: () => {
        this.notificacao.info('Convite cancelado.');
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }

  async remover(membro: Membro): Promise<void> {
    const confirmou = await this.confirmacao.confirmar({
      titulo: `Remover ${membro.nome}?`,
      mensagem:
        'A pessoa sai da equipe na hora e perde o acesso. O que ela já fez continua registrado com o nome dela.',
      confirmar: 'Remover',
      destrutivo: true,
    });
    if (!confirmou) {
      return;
    }
    this.equipeService.remover(membro.id).subscribe({
      next: () => {
        this.notificacao.info(`${membro.nome} saiu da equipe.`);
        this.carregar();
      },
      error: (e) => this.notificacao.erro(mensagemDeErro(e)),
    });
  }

  papel(membro: Membro): string {
    return membro.papel === 'PROPRIETARIO' ? 'Proprietário' : 'Membro';
  }

  private mensagemDoConvite(mensagem: string): string {
    return mensagem.toLowerCase().includes('email já cadastrado')
      ? 'Esse e-mail já tem uma conta no portal. Convide outro e-mail.'
      : mensagem;
  }
}
