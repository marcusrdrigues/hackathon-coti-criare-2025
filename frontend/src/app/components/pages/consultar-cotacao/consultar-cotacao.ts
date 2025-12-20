import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Navbar } from '../../shared/navbar/navbar';

// Interface ajustada para a realidade do Back-end
interface cotacao {
  id: number;
  titulo: string;
  descricao: string;
  dataCriacao: Date;
  status: 'ABERTO' | 'ENCERRADO'; // Apenas os status reais
  qtdPropostas: number;
}

@Component({
  selector: 'app-consultar-cotacao',
  standalone: true,
  imports: [
    Navbar,
    CommonModule
  ],
  templateUrl: './consultar-cotacao.html',
  styleUrl: './consultar-cotacao.css',
})
export class ConsultarCotacao {

  // Dados atualizados
  solicitacoes: cotacao[] = [
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
      status: 'ABERTO', // Mudado de EM_ANALISE para ABERTO
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

  // Função simplificada
  getStatusClass(status: string): string {
    switch (status) {
      case 'ABERTO': 
        return 'bg-success'; // Verde para chamar atenção
      case 'FECHADO': 
        return 'bg-secondary'; // Cinza para indicar inativo
      default: 
        return 'bg-primary';
    }
  }
}