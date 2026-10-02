import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { organizacaoGuard, perfilGuard, superadminGuard, visitanteGuard } from './core/auth.guards';
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

  {
    path: 'pages/esqueci-a-senha',
    canActivate: [visitanteGuard],
    title: 'Redefinir senha | Portal Criare',
    loadComponent: () => import('./components/pages/esqueci-senha/esqueci-senha').then((m) => m.EsqueciSenha),
  },

  // Link do e-mail de redefinição: público, com o token depois do "#" (spec 004)
  {
    path: 'redefinir-senha',
    title: 'Senha nova | Portal Criare',
    loadComponent: () => import('./components/pages/redefinir-senha/redefinir-senha').then((m) => m.RedefinirSenha),
  },

  // Convite para a equipe: público, aberto pelo link (o token vem depois do "#")
  {
    path: 'convite',
    title: 'Convite | Portal Criare',
    loadComponent: () => import('./components/pages/convite/convite').then((m) => m.ConviteComponent),
  },

  // Telas internas: todas dentro da estrutura (barra lateral no computador, abas no celular)
  {
    path: 'pages',
    canActivate: [perfilGuard()],
    loadComponent: () => import('./components/shared/shell/shell').then((m) => m.Shell),
    children: [
      // Superadmin: a área administrativa, sem as telas das organizações
      {
        path: 'admin',
        canActivate: [superadminGuard],
        title: 'Organizações | Portal Criare',
        loadComponent: () =>
          import('./components/pages/admin-organizacoes/admin-organizacoes').then((m) => m.AdminOrganizacoes),
      },

      // Os dois perfis
      {
        path: 'equipe',
        canActivate: [organizacaoGuard],
        title: 'Equipe | Portal Criare',
        loadComponent: () => import('./components/pages/equipe/equipe').then((m) => m.Equipe),
      },

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
        canActivate: [organizacaoGuard],
        title: 'Negociações | Portal Criare',
        loadComponent: () => import('./components/pages/negociacoes/negociacoes').then((m) => m.Negociacoes),
      },
      {
        path: 'negociacao/:id',
        canActivate: [organizacaoGuard],
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
