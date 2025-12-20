import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms'; // <--- Necessário para usar [(ngModel)]
import { Navbar } from "../../shared/navbar/navbar";

@Component({
  selector: 'app-cadastro-cotacao',
  standalone: true,
  imports: [
    Navbar, 
    CommonModule, 
    FormsModule // <--- Não esqueça de importar isso!
  ],
  templateUrl: './cadastro-cotacao.html',
  styleUrl: './cadastro-cotacao.css',
})
export class CadastroCotacao {

  // Objeto para armazenar os dados do formulário
  novaCotacao = {
    titulo: '',
    descricao: '',
    dataCriacao: this.getDataAtual(), // Já inicia com a data de hoje
    dataLimite: ''
  };

  // Função auxiliar para pegar a data de hoje no formato YYYY-MM-DD (padrão do input date)
  private getDataAtual(): string {
    return new Date().toISOString().split('T')[0];
  }

  // Função para simular o envio
  salvarCotacao() {
    console.log('Dados para envio:', this.novaCotacao);
    alert('Cotação salva com sucesso! (Olhe o console)');
    // Aqui você chamaria seu serviço de backend futuramente
  }
}