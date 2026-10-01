import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localePt from '@angular/common/locales/pt';
import {
  ApplicationConfig,
  DEFAULT_CURRENCY_CODE,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { AuthService } from './core/services/auth.service';

// Datas e moedas no formato brasileiro (R$ 1.234,56 / 31/12/2025)
registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // Antes da primeira rota: tenta restaurar a sessão pelo cookie (F5 não desloga)
    provideAppInitializer(() => inject(AuthService).restaurarSessao()),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'BRL' },
  ],
};
