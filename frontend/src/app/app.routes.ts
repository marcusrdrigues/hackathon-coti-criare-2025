import { Routes } from '@angular/router';
import { DashboardComponent } from './components/pages/dashboard/dashboard';
import { DashboardFornecedor } from './components/pages/dashboard-fornecedor/dashboard-fornecedor';
import { CadastroCotacao } from './components/pages/cadastro-cotacao/cadastro-cotacao';
import { NotFound } from './components/pages/not-found/not-found';
import { ConsultarCotacao } from './components/pages/consultar-cotacao/consultar-cotacao';


export const routes: Routes = [
    { path: '', pathMatch: 'full', redirectTo: '/pages/dashboard' },

    { path: 'pages/dashboard', component: DashboardComponent },
    { path: 'pages/dashboard-fornecedor', component: DashboardFornecedor },
    { path: 'pages/cadastro-cotacao', component: CadastroCotacao },
    { path: 'pages/consultar-cotacao', component: ConsultarCotacao },
    { path: '**', component: NotFound }
];
