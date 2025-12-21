import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HttpClientModule], 
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  credenciais = {
    email: '',
    senha: ''
  };

  constructor(private authService: AuthService, private router: Router) {}

  fazerLogin() {
    if (!this.credenciais.email || !this.credenciais.senha) {
      alert('Preencha todos os campos!');
      return;
    }

    console.log('Tentando logar...', this.credenciais);

    this.authService.login(this.credenciais).subscribe({
      next: (resposta) => {
        console.log('Sucesso! Resposta do Java:', resposta);

        // AQUI ACONTECE A MÁGICA DO REDIRECIONAMENTO
        if (resposta.tipo === 'EMPRESA') {
          this.router.navigate(['/pages/dashboard']); // Vai para Dashboard da Empresa
        } else if (resposta.tipo === 'FORNECEDOR') {
          this.router.navigate(['/pages/dashboard-fornecedor']); // Vai para Dashboard do Fornecedor
        } else {
          // Se o tipo vier estranho, manda pra empresa por padrão
          this.router.navigate(['/pages/dashboard']); 
        }
      },
      error: (erro) => {
        console.error('Erro no login:', erro);
        alert('Falha ao entrar. Verifique email e senha.');
      }
    });
  }
}