import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../shared/navbar/navbar';

@Component({
  selector: 'app-historico-propostas',
  standalone: true,
  imports: [Navbar, CommonModule],
  templateUrl: './historico-propostas.html',
  styleUrls: ['./historico-propostas.css']
})
export class historicoPropostas {

  historico = [
    {
      data: '10/11/2025',
      titulo: 'Fornecimento de Papel A4',
      empresa: 'Escritório Central',
      ganhou: true,
      valor: 'R$ 1.200,00'
    },
    {
      data: '05/10/2025',
      titulo: 'Instalação de Redes',
      empresa: 'Tech Inovações',
      ganhou: false,
      valor: 'R$ 4.500,00'
    },
    {
      data: '20/09/2025',
      titulo: 'Kit Boas Vindas',
      empresa: 'Criare Consulting',
      ganhou: true,
      valor: 'R$ 800,00'
    }
  ];
}