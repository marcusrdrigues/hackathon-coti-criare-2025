import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Notificacao, NotificacaoService } from '../../../core/services/notificacao.service';
import { Icone, NomeIcone } from '../../../ui/icone';

/** Avisos rápidos depois de uma ação, embaixo e no centro da tela. */
@Component({
  selector: 'app-toasts',
  imports: [Icone],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './toasts.html',
  styleUrl: './toasts.css',
})
export class Toasts {
  protected readonly notificacao = inject(NotificacaoService);

  protected readonly icones: Record<Notificacao['tipo'], NomeIcone> = {
    sucesso: 'sucesso',
    erro: 'alerta',
    info: 'info',
  };
}
