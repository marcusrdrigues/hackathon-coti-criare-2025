import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Navbar } from "../../shared/navbar/navbar"; // <--- IMPORTANTE

@Component({
  selector: 'app-detalhe-cotacao',
  standalone: true,
  imports: [
    Navbar,
    CommonModule, 
    RouterLink
  ],
  templateUrl: './detalhe-cotacao.html',
  styleUrl: './detalhe-cotacao.css'
})
export class DetalheCotacao {

  // DADOS DA COTAÇÃO (MOCK)
  cotacao = {
    id: 1,
    titulo: 'Aquisição de 10 Notebooks Dell Latitude',
    dataCriacao: '15/12/2025',
    melhorOferta: 'R$ 9.500,00',
    status: 'ABERTO'
  };

  // LISTA DE PROPOSTAS (MOCK)
  propostas = [
    {
      id: 101,
      fornecedor: 'Tech Soluções Ltda',
      cnpj: '12.345.678/0001-00',
      valorInicial: 'R$ 12.000,00',
      ultimaOferta: 'R$ 10.000,00',
      status: 'EM NEGOCIACAO'
    },
    {
      id: 102,
      fornecedor: 'InfoWorld Distribuidora',
      cnpj: '98.765.432/0001-99',
      valorInicial: 'R$ 9.500,00',
      ultimaOferta: 'R$ 9.500,00',
      status: 'NOVA PROPOSTA'
    },
    {
      id: 103,
      fornecedor: 'Fast Computadores',
      cnpj: '45.123.789/0001-55',
      valorInicial: 'R$ 15.000,00',
      ultimaOferta: 'R$ 14.500,00',
      status: 'RECUSADO'
    }
  ];

  // Função simples para colorir os badges
  getBadgeClass(status: string): string {
    switch (status) {
      case 'EM NEGOCIACAO': return 'badge-negociacao'; // Amarelo
      case 'NOVA PROPOSTA': return 'badge-nova';       // Azul Ciano
      case 'RECUSADO': return 'badge-recusada';        // Cinza
      default: return 'bg-secondary';
    }
  }
}