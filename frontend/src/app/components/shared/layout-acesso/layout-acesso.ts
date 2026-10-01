import { Component, input } from '@angular/core';
import { SeletorTema } from '../seletor-tema/seletor-tema';

/**
 * Moldura das telas de acesso (login e cadastro): painel da marca à esquerda
 * e o conteúdo da tela à direita. Em telas estreitas fica só o conteúdo, com a
 * marca compacta no topo. Os estilos do formulário ficam no styles.css global
 * (seção "Telas de acesso"), porque o conteúdo é projetado por cada página.
 */
@Component({
  selector: 'app-layout-acesso',
  imports: [SeletorTema],
  templateUrl: './layout-acesso.html',
  styleUrl: './layout-acesso.css',
})
export class LayoutAcesso {
  readonly titulo = input('Do pedido de compra ao negócio fechado, numa conversa só.');
  readonly texto = input(
    'Empresas publicam cotações, fornecedores enviam propostas e as duas partes negociam o valor final com todo o histórico registrado.',
  );
}
