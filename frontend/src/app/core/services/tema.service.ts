import { computed, effect, Injectable, signal } from '@angular/core';

export type PreferenciaTema = 'sistema' | 'claro' | 'escuro';

const CHAVE = 'tema';
const CONSULTA_ESCURO = '(prefers-color-scheme: dark)';

/**
 * Tema claro/escuro.
 *
 * Por padrão segue o sistema operacional e muda junto com ele. A pessoa pode
 * fixar "claro" ou "escuro"; a escolha fica no localStorage. O mesmo cálculo é
 * feito por um script no index.html antes do Angular carregar, para a página
 * já abrir no tema certo, sem piscar.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly midia =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia(CONSULTA_ESCURO)
      : null;

  private readonly sistemaEscuro = signal(this.midia?.matches ?? false);

  readonly preferencia = signal<PreferenciaTema>(lerPreferencia());

  readonly escuro = computed(
    () => this.preferencia() === 'escuro' || (this.preferencia() === 'sistema' && this.sistemaEscuro()),
  );

  constructor() {
    this.midia?.addEventListener('change', (evento) => this.sistemaEscuro.set(evento.matches));

    effect(() => {
      document.documentElement.setAttribute('data-bs-theme', this.escuro() ? 'dark' : 'light');
    });
  }

  definir(preferencia: PreferenciaTema): void {
    this.preferencia.set(preferencia);
    try {
      if (preferencia === 'sistema') {
        localStorage.removeItem(CHAVE);
      } else {
        localStorage.setItem(CHAVE, preferencia);
      }
    } catch {
      // Navegação privada ou armazenamento bloqueado: vale só para esta visita.
    }
  }
}

function lerPreferencia(): PreferenciaTema {
  try {
    const salva = localStorage.getItem(CHAVE);
    return salva === 'claro' || salva === 'escuro' ? salva : 'sistema';
  } catch {
    return 'sistema';
  }
}
