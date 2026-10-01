import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from "../../shared/navbar/navbar";
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard-fornecedor',
  standalone: true,
  imports: [Navbar, CommonModule, RouterLink],
  templateUrl: './dashboard-fornecedor.html',
  styleUrl: './dashboard-fornecedor.css'
})
export class DashboardFornecedor {

  // KPIs: Foco em "O que eu posso ganhar?" e "O que eu já ganhei?"
  kpis = {
    oportunidades: 12,    // Cotações abertas no sistema (Azul)
    enviadas: 5,          // Propostas que enviei (Amarelo)
    ganhas: 2             // Propostas aceitas (Verde - Money!)
  };

  // Tabela: Mural de oportunidades recentes
  oportunidadesRecentes = [
    { 
      id: 101, 
      empresa: 'Tech Inovações', 
      titulo: 'Compra de 20 Monitores 24"', 
      encerramento: new Date('2025-12-25'),
      valorTeto: 25000.00 // Opcional, se quiser mostrar
    },
    { 
      id: 102, 
      empresa: 'Hospital Central', 
      titulo: 'Serviço de Manutenção AC', 
      encerramento: new Date('2025-12-22'),
      valorTeto: null // Sem valor revelado
    },
    { 
      id: 103, 
      empresa: 'Logística Express', 
      titulo: 'Fornecimento de Caixas de Papelão', 
      encerramento: new Date('2025-12-28'),
      valorTeto: 5000.00
    }
  ];

  // Helper para calcular dias restantes
  getDiasRestantes(data: Date): number {
    const diff = data.getTime() - new Date().getTime();
    return Math.ceil(diff / (1000 * 3600 * 24));
  }
}