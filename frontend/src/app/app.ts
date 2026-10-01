import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toasts } from './components/shared/toasts/toasts';
import { TemaService } from './core/services/tema.service';
import { Confirmacao } from './ui/confirmacao';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts, Confirmacao],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  constructor() {
    // Criado aqui para acompanhar mudanças do tema do sistema em qualquer tela
    inject(TemaService);
  }
}
