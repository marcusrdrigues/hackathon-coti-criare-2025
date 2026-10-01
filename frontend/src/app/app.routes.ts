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

  // Acesso
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

  // Empresa
  {
    path: 'pages/dashboard',
    canActivate: [empresa],
    title: 'Visão geral | Portal Criare',
    loadComponent: () => import('./components/pages/dashboard/dashboard').then((m) => m.DashboardComponent),
  },
  {
    path: 'pages/cadastro-cotacao',
    canActivate: [empresa],
    title: 'Nova cotação | Portal Criare',
    loadComponent: () => import('./components/pages/cadastro-cotacao/cadastro-cotacao').then((m) => m.CadastroCotacao),
  },
  {
    path: 'pages/cadastro-cotacao/:id',
    canActivate: [empresa],
    title: 'Editar cotação | Portal Criare',
    loadComponent: () => import('./components/pages/cadastro-cotacao/cadastro-cotacao').then((m) => m.CadastroCotacao),
  },
  {
    path: 'pages/consultar-cotacao',
    canActivate: [empresa],
    title: 'Minhas cotações | Portal Criare',
    loadComponent: () => import('./components/pages/consultar-cotacao/consultar-cotacao').then((m) => m.ConsultarCotacao),
  },
  {
    path: 'pages/detalhe-cotacao/:id',
    canActivate: [empresa],
    title: 'Cotação | Portal Criare',
    loadComponent: () => import('./components/pages/detalhe-cotacao/detalhe-cotacao').then((m) => m.DetalheCotacao),
  },

  // Fornecedor
  {
    path: 'pages/dashboard-fornecedor',
    canActivate: [fornecedor],
    title: 'Painel | Portal Criare',
    loadComponent: () =>
      import('./components/pages/dashboard-fornecedor/dashboard-fornecedor').then((m) => m.DashboardFornecedor),
  },
  {
    path: 'pages/mural-oportunidades',
    canActivate: [fornecedor],
    title: 'Mural de cotações | Portal Criare',
    loadComponent: () =>
      import('./components/pages/mural-oportunidades/mural-oportunidades').then((m) => m.MuralOportunidades),
  },
  {
    path: 'pages/propostas-enviadas',
    canActivate: [fornecedor],
    title: 'Propostas em andamento | Portal Criare',
    loadComponent: () =>
      import('./components/pages/propostas-enviadas/propostas-enviadas').then((m) => m.PropostasEnviadas),
  },
  {
    path: 'pages/historico-propostas',
    canActivate: [fornecedor],
    title: 'Histórico | Portal Criare',
    loadComponent: () =>
      import('./components/pages/historico-propostas/historico-propostas').then((m) => m.HistoricoPropostas),
  },

  // Os dois perfis
  {
    path: 'pages/negociacao/:id',
    canActivate: [perfilGuard()],
    title: 'Negociação | Portal Criare',
    loadComponent: () => import('./components/pages/negociacao/negociacao').then((m) => m.Negociacao),
  },

  {
    path: '**',
    title: 'Página não encontrada | Portal Criare',
    loadComponent: () => import('./components/pages/not-found/not-found').then((m) => m.NotFound),
  },
];
