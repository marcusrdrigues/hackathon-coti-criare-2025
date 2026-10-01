import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css'
})
export class CadastroComponent {

  // O ID será gerado pelo Backend (Java), então não precisamos enviar
  registro = {
    nome: '',
    cnpj: '',
    email: '',
    senha: '', // Adicionei para poder fazer login depois
    tipo: 'FORNECEDOR' // Padrão
  };

  constructor(private router: Router) {}

  // Alterna o visual e o valor do tipo
  setTipo(tipo: string) {
    this.registro.tipo = tipo;
  }

  cadastrar() {
    console.log('Enviando cadastro:', this.registro);
    
    // Validação simples para Hackathon
    if(this.registro.nome && this.registro.email && this.registro.senha) {
      alert('Cadastro realizado com sucesso!');
      this.router.navigate(['/pages/login']); // Manda pro login
    } else {
      alert('Preencha todos os campos!');
    }
  }
}