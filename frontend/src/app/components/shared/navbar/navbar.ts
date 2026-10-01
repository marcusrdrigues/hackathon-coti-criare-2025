import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CnpjPipe } from '../../../core/utils/cnpj.pipe';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, CnpjPipe],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  protected readonly auth = inject(AuthService);

  protected readonly usuario = this.auth.usuario;
  protected readonly rotaDashboard = computed(() => this.auth.rotaInicial());
  protected readonly tipoPerfilTexto = computed(() =>
    this.auth.ehEmpresa() ? 'Perfil Corporativo' : 'Portal do Fornecedor',
  );

  sair(): void {
    this.auth.logout();
  }
}
