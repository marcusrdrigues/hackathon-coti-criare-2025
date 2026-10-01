import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { perfilGuard, visitanteGuard } from './core/auth.guards';
import { AuthService } from './core/services/auth.service';

const empresa = perfilGuard('EMPRESA');
const fornecedor = perfilGuard('FORNECEDOR');

// Cada tela é carregada sob demanda (lazy loading): o usuário baixa só o que usa.

export const routes: Routes = [
  // Raiz: manda cada perfil para o seu painel (ou para o login)
  { path: '', pathMatch: 'full', redirectTo: () => inject(AuthService).rotaInicial() },

  // Acesso (fora da estrutura com barra lateral)
  {
    path: 'pages/login',
    canActivate: [visitanteGuard],
    title: 'Entrar | Portal Criare',
    loadComponent: () => import('./components/pages/login/login').then((m) => m.Login),
  },
  {
    path: 'pages/cadastro',
    canActivate: [visitanteGuard],
    title: 'Cadastro | Portal Criare',
    loadComponent: () => import('./components/pages/cadastro/cadastro').then((m) => m.CadastroComponent),
  },

  // Telas internas: todas dentro da estrutura (barra lateral no computador, abas no celular)
  {
    path: 'pages',
    canActivate: [perfilGuard()],
    loadComponent: () => import('./components/shared/shell/shell').then((m) => m.Shell),
    children: [
      // Empresa
      {
        path: 'dashboard',
        canActivate: [empresa],
        title: 'Início | Portal Criare',
        loadComponent: () => import('./components/pages/dashboard/dashboard').then((m) => m.DashboardComponent),
      },
      {
        path: 'cadastro-cotacao',
        canActivate: [empresa],
        title: 'Nova cotação | Portal Criare',
        loadComponent: () =>
          import('./components/pages/cadastro-cotacao/cadastro-cotacao').then((m) => m.CadastroCotacao),
      },
      {
        path: 'cadastro-cotacao/:id',
        canActivate: [empresa],
        title: 'Editar cotação | Portal Criare',
        loadComponent: () =>
          import('./components/pages/cadastro-cotacao/cadastro-cotacao').then((m) => m.CadastroCotacao),
      },
      {
        path: 'consultar-cotacao',
        canActivate: [empresa],
        title: 'Cotações | Portal Criare',
        loadComponent: () =>
          import('./components/pages/consultar-cotacao/consultar-cotacao').then((m) => m.ConsultarCotacao),
      },
      {
        path: 'detalhe-cotacao/:id',
        canActivate: [empresa],
        title: 'Cotação | Portal Criare',
        loadComponent: () => import('./components/pages/detalhe-cotacao/detalhe-cotacao').then((m) => m.DetalheCotacao),
      },

      // Fornecedor
      {
        path: 'dashboard-fornecedor',
        canActivate: [fornecedor],
        title: 'Início | Portal Criare',
        loadComponent: () =>
          import('./components/pages/dashboard-fornecedor/dashboard-fornecedor').then((m) => m.DashboardFornecedor),
      },
      {
        path: 'mural-oportunidades',
        canActivate: [fornecedor],
        title: 'Mural | Portal Criare',
        loadComponent: () =>
          import('./components/pages/mural-oportunidades/mural-oportunidades').then((m) => m.MuralOportunidades),
      },
      {
        path: 'propostas-enviadas',
        canActivate: [fornecedor],
        title: 'Propostas | Portal Criare',
        loadComponent: () =>
          import('./components/pages/propostas-enviadas/propostas-enviadas').then((m) => m.PropostasEnviadas),
      },
      {
        path: 'historico-propostas',
        canActivate: [fornecedor],
        title: 'Histórico | Portal Criare',
        loadComponent: () =>
          import('./components/pages/historico-propostas/historico-propostas').then((m) => m.HistoricoPropostas),
      },

      // Os dois perfis
      {
        path: 'negociacoes',
        title: 'Negociações | Portal Criare',
        loadComponent: () => import('./components/pages/negociacoes/negociacoes').then((m) => m.Negociacoes),
      },
      {
        path: 'negociacao/:id',
        title: 'Negociação | Portal Criare',
        loadComponent: () => import('./components/pages/negociacao/negociacao').then((m) => m.Negociacao),
      },
    ],
  },

  {
    path: '**',
    title: 'Página não encontrada | Portal Criare',
    loadComponent: () => import('./components/pages/not-found/not-found').then((m) => m.NotFound),
  },
];
