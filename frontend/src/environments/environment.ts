/**
 * Configuração de produção (ng build).
 * A API é chamada no mesmo domínio do front-end: o vercel.json repassa /api/*
 * para o back-end. Assim o cookie da sessão é do próprio site e não há CORS.
 */
export const environment = {
  apiUrl: '/api/v1',
  // Tempo real: o proxy da Vercel não repassa WebSocket, então a conexão vai direto na API (ver docs/adr/0012)
  wsUrl: 'wss://portal-criare-api.onrender.com/ws',
};
