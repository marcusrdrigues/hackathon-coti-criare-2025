import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Navbar } from "../../shared/navbar/navbar";
import { Router } from '@angular/router';

@Component({
  selector: 'app-negociacao',
  standalone: true,
  imports: [Navbar, CommonModule, FormsModule],
  templateUrl: './negociacao.html',
  styleUrl: './negociacao.css'
})
export class Negociacao {
  
  valorContraposta: number | null = null;

  constructor(private router: Router) {}

  enviarContraproposta() {
    if(!this.valorContraposta) {
      alert("Digite um valor!");
      return;
    }
    // Aqui você conectaria com o Back, mas pro Hackathon:
    alert(`Contraproposta de R$ ${this.valorContraposta} enviada para o fornecedor!`);
    // Opcional: Recarregar ou adicionar na lista visualmente
    this.valorContraposta = null;
  }

  aceitar() {
    if(confirm('Tem certeza que deseja fechar negócio por R$ 20.000?')) {
      alert('Parabéns! Negócio fechado. Gerando contrato...');
      this.router.navigate(['/pages/dashboard']);
    }
  }

  recusar() {
    if(confirm('Deseja encerrar as negociações com este fornecedor?')) {
      this.router.navigate(['/pages/detalhe-cotacao/1']);
    }
  }
}