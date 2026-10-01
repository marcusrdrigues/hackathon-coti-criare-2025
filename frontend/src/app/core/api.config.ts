import { environment } from '../../environments/environment';

/**
 * Endereço base da API.
 * - desenvolvimento: http://localhost:8085/api/v1 (src/environments/environment.development.ts)
 * - produção: /api/v1, repassado ao back-end pelo vercel.json (src/environments/environment.ts)
 */
export const API_URL = environment.apiUrl;
