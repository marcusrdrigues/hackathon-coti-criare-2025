import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../shared/navbar/navbar';
import { Router } from '@angular/router';

@Component({
  selector: 'app-propostas-enviadas',
  standalone: true,
  imports: [Navbar, CommonModule],
  templateUrl: './propostas-enviadas.html',
  styleUrl: './propostas-enviadas.css'
})
export class PropostasEnviadas {

  enviadas = [
    {
      titulo: 'Compra de 20 Notebooks',
      empresa: 'Criare Consulting',
      valor: 'R$ 20.000,00',
      status: 'EM_NEGOCIACAO',
      statusTexto: 'Em Negociação'
    },
    {
      titulo: 'Serviço de Limpeza Pós-Obra',
      empresa: 'Construtora XYZ',
      valor: 'R$ 3.500,00',
      status: 'ENVIADO',
      statusTexto: 'Aguardando Análise'
    }
  ];

  constructor(private router: Router) {}

  abrirNegociacao() {
    // Reutiliza a tela de negociação, mas passando ID mockado
    // Como Fornecedor, a visão seria parecida
    this.router.navigate(['/pages/negociacao/1']);
  }
}