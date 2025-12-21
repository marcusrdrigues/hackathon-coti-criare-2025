import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterLink } from '@angular/router'; // Adicionei para o botão funcionar
import { Navbar } from '../../shared/navbar/navbar';

interface Cotacao {
  id: number;
  titulo: string;
  descricao: string;
  dataCriacao: Date;
  status: 'ABERTO' | 'ENCERRADO';
  qtdPropostas: number;
}

@Component({
  selector: 'app-consultar-cotacao',
  standalone: true,
  imports: [
    Navbar,
    CommonModule,
    RouterLink // Necessário para o botão "Nova Solicitação"
  ],
  templateUrl: './consultar-cotacao.html',
  styleUrl: './consultar-cotacao.css',
})
export class ConsultarCotacao {

  solicitacoes: Cotacao[] = [
    {
      id: 1,
      titulo: 'Aquisição de Notebooks',
      descricao: 'Precisamos de 10 unidades do modelo Latitude 5420 para o setor de TI.',
      dataCriacao: new Date(),
      status: 'ABERTO',
      qtdPropostas: 3
    },
    {
      id: 2,
      titulo: 'Serviço de Limpeza Pós-Obra',
      descricao: 'Limpeza completa do galpão B após reforma estrutural.',
      dataCriacao: new Date('2025-11-15'),
      status: 'ABERTO',
      qtdPropostas: 5
    },
    {
      id: 3,
      titulo: 'Montagem de Escritório Corporativo',
      descricao: 'Precisamos mobiliar o novo escritório com mesas, cadeiras e armários.',
      dataCriacao: new Date('2025-10-10'),
      status: 'ENCERRADO',
      qtdPropostas: 12
    }
  ];

  getStatusClass(status: string): string {
    switch (status) {
      case 'ABERTO': 
        return 'bg-success'; // Verde (Mantive verde pois "Aberto" é sinal positivo)
      case 'ENCERRADO': 
        return 'bg-secondary'; // Cinza
      default: 
        return 'bg-light text-dark';
    }
  }
}