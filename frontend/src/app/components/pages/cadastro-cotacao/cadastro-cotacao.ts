import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Navbar } from "../../shared/navbar/navbar";

@Component({
  selector: 'app-cadastro-cotacao',
  standalone: true,
  imports: [
    Navbar, 
    CommonModule, 
    FormsModule 
  ],
  templateUrl: './cadastro-cotacao.html',
  styleUrl: './cadastro-cotacao.css',
})
export class CadastroCotacao {

  novaCotacao = {
    titulo: '',
    descricao: '',
    dataCriacao: this.getDataAtual(),
    dataLimite: ''
  };

  constructor(private router: Router) {}

  private getDataAtual(): string {
    return new Date().toISOString().split('T')[0];
  }

  salvarCotacao() {
    console.log('Dados:', this.novaCotacao);
    alert('Cotação publicada com sucesso!'); // Pequeno ajuste no texto para combinar com o botão
    this.router.navigate(['/pages/dashboard']);
  }

  cancelar() {
    this.router.navigate(['/pages/dashboard']);
  }
}