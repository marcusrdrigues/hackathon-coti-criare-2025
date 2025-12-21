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
      valorProposta: '',
      descricaoPropostaTexto: ''
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
      valorProposta: '',
      descricaoPropostaTexto: ''
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
      valorProposta: '',
      descricaoPropostaTexto: ''
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
  // 1. Validação básica
  if (!item.valorProposta) {
    alert('Digite o valor!');
    return;
  }

  // 2. Pega o ID do Fornecedor Logado (do localStorage/AuthService)
  // Supondo que você tem o objeto usuario salvo
  const usuarioLogado = JSON.parse(localStorage.getItem('sessao_usuario') || '{}');

  // 3. MONTA O JSON EXATO QUE O JAVA ESPERA
  const propostaDTO = {
    descricao: item.descricaoPropostaTexto || 'Sem descrição adicional', // Campo novo
    valor: item.valorProposta,
    fornecedor: { id: usuarioLogado.id }, // O Java espera um objeto ou ID
    cotacao: { id: item.id }              // ID da cotação que você clicou
  };

  console.log('Enviando pro Java:', propostaDTO);

  // 4. AQUI ENTRA A CHAMADA PRO SERVICE (Quando tiver a API)
  // this.propostaService.criar(propostaDTO).subscribe(...)

  alert('Proposta enviada com sucesso!');
  item.expandido = false;
}
}