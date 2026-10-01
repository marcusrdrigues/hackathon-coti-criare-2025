import { Component, inject } from '@angular/core';
import { NotificacaoService } from '../../../core/services/notificacao.service';

@Component({
  selector: 'app-toasts',
  templateUrl: './toasts.html',
  styleUrl: './toasts.css',
})
export class Toasts {
  protected readonly notificacao = inject(NotificacaoService);

  protected readonly estilos = {
    sucesso: { classe: 'text-bg-success', icone: 'bi-check-circle-fill' },
    erro: { classe: 'text-bg-danger', icone: 'bi-exclamation-octagon-fill' },
    info: { classe: 'text-bg-dark', icone: 'bi-info-circle-fill' },
  };
}
