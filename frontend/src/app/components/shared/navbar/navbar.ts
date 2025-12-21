import { Component, OnInit } from '@angular/core';
import { RouterLink, Router } from '@angular/router'; // Adicionei Router aqui
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar implements OnInit {
  
  // Mocks
  perfilEmpresa = {
    nome: 'Criare Consulting (Dev)',
    tipo: 'EMPRESA',
    cnpj: '12.345.678/0001-99'
  };

  perfilFornecedor = {
    nome: 'Tech Soluções (Fornecedor)',
    tipo: 'FORNECEDOR',
    cnpj: '98.765.432/0001-00'
  };

  // Começa nulo, vamos decidir no ngOnInit
  usuario: any = null; 

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    // 1. Verifica se já temos uma "escolha forçada" salva no navegador
    const modoSalvo = localStorage.getItem('modo_demo');

    if (modoSalvo === 'EMPRESA') {
      this.usuario = this.perfilEmpresa;
    } else if (modoSalvo === 'FORNECEDOR') {
      this.usuario = this.perfilFornecedor;
    } else {
      // Se não tiver nada salvo, assume Fornecedor por padrão
      this.usuario = this.perfilFornecedor;
    }
    
    // (Opcional) Se tiver login real, sobrescreve o mock
    this.authService.usuario$.subscribe(dados => {
      if (dados) this.usuario = dados;
    });
  }

  alternarPerfil() {
    if (this.usuario.tipo === 'EMPRESA') {
      // Muda para Fornecedor
      this.usuario = this.perfilFornecedor;
      localStorage.setItem('modo_demo', 'FORNECEDOR'); // <--- SALVA NA MEMÓRIA
      alert('Alternado para FORNECEDOR');
      this.router.navigate(['/pages/dashboard-fornecedor']); // Já manda pra tela certa
    } else {
      // Muda para Empresa
      this.usuario = this.perfilEmpresa;
      localStorage.setItem('modo_demo', 'EMPRESA'); // <--- SALVA NA MEMÓRIA
      alert('Alternado para EMPRESA');
      this.router.navigate(['/pages/dashboard']); // Já manda pra tela certa
    }
  }

  get tipoPerfilTexto(): string {
    return this.usuario?.tipo === 'EMPRESA' ? 'Perfil Corporativo' : 'Portal do Fornecedor';
  }

  get rotaDashboard(): string {
    if (this.usuario && this.usuario.tipo === 'FORNECEDOR') {
      return '/pages/dashboard-fornecedor'; // Se não tiver essa rota, use /pages/mural-oportunidades
    }
    return '/pages/dashboard';
  }

  sair() {
    this.authService.logout();
    localStorage.removeItem('modo_demo'); // Limpa a memória ao sair
  }
}