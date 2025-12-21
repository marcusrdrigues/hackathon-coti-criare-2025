import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // <--- IMPORTANTE: Adicione o FormsModule
import { Navbar } from '../../shared/navbar/navbar';
import { Router } from '@angular/router';

@Component({
  selector: 'app-mural-cotacoes',
  standalone: true,
  imports: [Navbar, CommonModule, FormsModule], // <--- Adicione FormsModule aqui também
  templateUrl: './mural-oportunidades.html',
  styleUrl: './mural-oportunidades.css'
})
export class MuralOportunidades {

  oportunidades = [
    {
      id: 1,
      titulo: 'Manutenção de Ar Condicionado (15 unidades)',
      empresa: 'Hospital Santa Vida',
      descricao: 'Busco empresa especializada...',
      local: 'Rio de Janeiro, RJ',
      dataLimite: '25/12/2025',
      orcamentoEstimado: 'Até R$ 5.000,00',
      
      // NOVOS CAMPOS DE CONTROLE (MOCK)
      expandido: false,
      valorProposta: '' 
    },
    {
      id: 2,
      titulo: 'Fornecimento de Café e Descartáveis',
      empresa: 'Criare Consulting',
      descricao: 'Contrato mensal para fornecimento...',
      local: 'São Paulo, SP',
      dataLimite: '30/12/2025',
      orcamentoEstimado: 'Sob Consulta',
      
      expandido: false,
      valorProposta: ''
    },
    {
      id: 3,
      titulo: 'Desenvolvimento de Landing Page',
      empresa: 'Tech Startups',
      descricao: 'Criação de página institucional...',
      local: 'Remoto',
      dataLimite: '22/12/2025',
      orcamentoEstimado: 'R$ 2.000,00',
      
      expandido: false,
      valorProposta: ''
    }
  ];

  constructor(private router: Router) {}

  diasRestantes(dataStr: string): number {
    return 5; 
  }

  // 1. Função que apenas ABRE o painelzinho
  toggleProposta(item: any) {
    // Fecha os outros para não ficar bagunçado (opcional)
    this.oportunidades.forEach(op => {
      if (op !== item) op.expandido = false;
    });

    item.expandido = !item.expandido;
  }

  // 2. Função que ENVIA de verdade
  confirmarEnvio(item: any) {
    if (!item.valorProposta) {
      alert('Por favor, digite um valor para a proposta.');
      return;
    }

    alert(`Sucesso! Proposta de R$ ${item.valorProposta} enviada para ${item.empresa}!`);
    
    // Reseta o card
    item.expandido = false;
    item.valorProposta = '';
    
    // Redireciona
    this.router.navigate(['/pages/propostas-enviadas']);
  }
}