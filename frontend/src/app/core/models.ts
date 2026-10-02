// Tipos espelhando os DTOs da API (pacotes aplicacao.dto de cada módulo do back-end).
// Quem é o usuário (a pessoa, a organização dela e o papel) a API descobre pelo token.

/** O lado da organização no negócio: quem compra ou quem fornece. */
export type TipoOrganizacao = 'EMPRESA' | 'FORNECEDOR';
/** O que a pessoa pode fazer: dentro da organização, ou na plataforma (superadmin, sem organização). */
export type Papel = 'PROPRIETARIO' | 'MEMBRO' | 'SUPERADMIN';
export type StatusCotacao = 'ABERTA' | 'EM_NEGOCIACAO' | 'FECHADA' | 'CANCELADA';
export type StatusProposta = 'ENVIADA' | 'EM_ANALISE' | 'ACEITA' | 'RECUSADA';
export type StatusNegociacao = 'EM_ANDAMENTO' | 'FINALIZADA' | 'CANCELADA';
export type CategoriaCotacao =
  | 'TECNOLOGIA'
  | 'MOBILIARIO'
  | 'LIMPEZA_MANUTENCAO'
  | 'ALIMENTOS'
  | 'SUPRIMENTOS'
  | 'SERVICOS'
  | 'OUTROS';

/**
 * A pessoa que entrou no portal e a organização em nome de quem ela age.
 * O superadmin não tem organização: `tipo` e `organizacao` vêm nulos.
 */
export interface Usuario {
  id: string;
  nome: string;
  email: string;
  tipo: TipoOrganizacao | null;
  papel: Papel;
  organizacao: OrganizacaoResumo | null;
}

/** Uma página de uma lista paginada da API (mesmo formato do PagedModel do Spring Data). */
export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Uma organização vista pelo superadmin, com os totais de uso. */
export interface OrganizacaoAdmin {
  id: string;
  tipo: TipoOrganizacao;
  razaoSocial: string;
  cnpj: string;
  criadaEm: string;
  pessoas: number;
  cotacoes: number;
  propostas: number;
}

export interface OrganizacaoResumo {
  id: string;
  razaoSocial: string;
  cnpj: string;
}

/** Resposta de login e de renovação. O refresh token vai num cookie HttpOnly, não aqui. */
export interface TokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  usuario: Usuario;
}

/** Conta de demonstração (só existe quando a API roda com o profile "demo"). */
export interface ContaDemo {
  perfil: TipoOrganizacao;
  nome: string;
  descricao: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

/** Cadastro público: a organização e a pessoa que vai ser a proprietária dela. */
export interface CadastroRequest {
  tipo: TipoOrganizacao;
  razaoSocial: string;
  cnpj: string;
  nome: string;
  email: string;
  senha: string;
}

export interface Categoria {
  codigo: CategoriaCotacao;
  descricao: string;
}

export interface Cotacao {
  id: string;
  nomeServico: string;
  requisitos: string;
  categoria: CategoriaCotacao | null;
  categoriaDescricao: string | null;
  orcamentoEstimado: number | null;
  dataCriacao: string;
  dataLimite: string | null;
  status: StatusCotacao;
  empresaId: string;
  empresaNome: string;
  quantidadePropostas: number;
  /** Menor valor entre as propostas em jogo; só vem para a empresa dona. */
  melhorOferta: number | null;
  /** A proposta que o fornecedor logado enviou para esta cotação, se houver. */
  minhaProposta: { id: string; valor: number; status: StatusProposta } | null;
}

/** Filtros das listas paginadas. */
export interface FiltroCotacoes {
  status?: StatusCotacao | null;
  categoria?: CategoriaCotacao | null;
  busca?: string;
  sort?: string;
}
export type SituacaoProposta = 'ANDAMENTO' | 'HISTORICO';
export type SituacaoNegociacao = 'ANDAMENTO' | 'TODAS';

/** Mensagens não lidas: o total e por negociação (só as que têm alguma). */
export interface NaoLidas {
  total: number;
  porNegociacao: Record<string, number>;
}

export interface CotacaoRequest {
  nomeServico: string;
  requisitos: string;
  categoria: CategoriaCotacao | null;
  orcamentoEstimado: number | null;
  dataLimite: string | null;
}

export interface Proposta {
  id: string;
  valor: number;
  descricao: string;
  status: StatusProposta;
  dataEnvio: string | null;
  fornecedorId: string;
  fornecedorNome: string;
  fornecedorCnpj: string;
  cotacaoId: string;
  cotacaoNome: string;
  cotacaoStatus: StatusCotacao;
  empresaId: string;
  empresaNome: string;
  negociacaoId: string | null;
  negociacaoStatus: StatusNegociacao | null;
  valorFinal: number | null;
}

export interface PropostaRequest {
  valor: number;
  descricao: string;
  cotacaoId: string;
}

export interface Negociacao {
  id: string;
  valorFinal: number | null;
  status: StatusNegociacao;
  dataInicio: string;
  dataFinalizacao: string | null;
  empresaId: string;
  empresaNome: string;
  fornecedorId: string;
  fornecedorNome: string;
  propostaId: string;
  valorProposta: number;
  ultimaOferta: number;
  cotacaoId: string;
  cotacaoNome: string;
  cotacaoRequisitos: string;
  cotacaoStatus: StatusCotacao;
  /** Mensagens da outra parte que o usuário ainda não viu (só nas consultas do próprio usuário) */
  naoLidas?: number | null;
}

export interface Mensagem {
  id: string;
  mensagem: string;
  valorOfertado: number | null;
  tipoRemetente: TipoOrganizacao;
  /** A pessoa que escreveu */
  remetenteId: string;
  /** A organização dela */
  remetenteNome: string;
  /** O nome da pessoa */
  remetentePessoa: string;
  dataEnvio: string;
  negociacaoId: string;
}

export interface MensagemRequest {
  negociacaoId: string;
  mensagem: string | null;
  valorOfertado: number | null;
}

export interface CategoriaResumo {
  categoria: CategoriaCotacao;
  descricao: string;
  total: number;
  percentual: number;
}

export interface FornecedorResumo {
  id: string;
  nome: string;
  cnpj: string;
  totalPropostas: number;
}

export interface DashboardEmpresa {
  cotacoesAbertas: number;
  cotacoesEmNegociacao: number;
  cotacoesFechadas: number;
  propostasRecebidas: number;
  categorias: CategoriaResumo[];
  topFornecedores: FornecedorResumo[];
}

export interface DashboardFornecedor {
  oportunidadesAbertas: number;
  propostasPendentes: number;
  negociacoesAtivas: number;
  cotacoesGanhas: number;
  valorTotalGanho: number;
}

/** Uma pessoa da equipe. O id é o do vínculo com a organização (é ele que se remove). */
export interface Membro {
  id: string;
  nome: string;
  email: string;
  papel: Papel;
  desde: string;
  /** A própria pessoa que está vendo a lista */
  voce: boolean;
}

/** Convite pendente. O token só vem na resposta de quem acabou de criar o convite. */
export interface Convite {
  id: string;
  nome: string;
  email: string;
  convidadoPor: string;
  expiraEm: string;
  token: string | null;
}

export interface ConviteRequest {
  nome: string;
  email: string;
}

/** O que a tela de aceitar convite mostra antes de a pessoa definir a senha. */
export interface ConviteAberto {
  organizacao: string;
  tipo: TipoOrganizacao;
  convidadoPor: string;
  nome: string;
  email: string;
  expiraEm: string;
}
