import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common'; // Essencial para o *ngIf

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  
  // Defina o tipo aqui. No futuro, isso virá do seu LoginService ou LocalStorage
  // Opções: 'EMPRESA' | 'FORNECEDOR'
  usuario = {
    nome: 'Tech Inovações',
    tipo: 'EMPRESA', 
    foto: null
  };

  // Helper para o texto do perfil
  get tipoPerfilTexto(): string {
    return this.usuario.tipo === 'EMPRESA' ? 'Perfil Corporativo' : 'Portal do Fornecedor';
  }

  // MÉTODO PARA DEMONSTRAÇÃO NO HACKATHON
  // (Você pode ligar isso num botão escondido ou no console para mostrar aos juízes)
  alternarPerfil() {
    if (this.usuario.tipo === 'EMPRESA') {
      this.usuario.tipo = 'FORNECEDOR';
      this.usuario.nome = 'Fornecedor João';
    } else {
      this.usuario.tipo = 'EMPRESA';
      this.usuario.nome = 'Tech Inovações';
    }
  }
}