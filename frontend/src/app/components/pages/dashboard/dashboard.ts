import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { Navbar } from "../../shared/navbar/navbar";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    Navbar, 
    CommonModule
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent {

  // DADOS DOS CARDS DO TOPO (KPIs)
  kpis = {
    cotacoesAbertas: 2,      // Status: ABERTO
    propostasRecebidas: 20,  // Volume total
    economiaGerada: 1000.00 // Resultado das cotacoes FECHADAS
  };

  // LISTA DE FORNECEDORES
  topFornecedores = [
    { nome: 'Tech Soluções', categoria: 'TI' },
    { nome: 'Limpeza', categoria: 'Serviços' },
    { nome: 'Cadeiras', categoria: 'Móveis' },
    { nome: 'Papelaria', categoria: 'Suprimentos' },
    { nome: 'Café', categoria: 'Alimentos' }
  ];

  // DADOS DO GRÁFICO
  categoriasStats = [
    { label: 'Informática e TI', porcentagem: 75, cor: 'bg-primary' },
    { label: 'Mobiliário', porcentagem: 50, cor: 'bg-success' },
    { label: 'Limpeza e Manutenção', porcentagem: 30, cor: 'bg-info' },
    { label: 'Copa e Cozinha', porcentagem: 20, cor: 'bg-warning' }
  ];
}