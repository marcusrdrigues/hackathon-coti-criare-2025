// Tipos espelhando os DTOs da API (pacote com.gestao.dtos).

export type TipoUsuario = 'EMPRESA' | 'FORNECEDOR';
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

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  cnpj: string;
  tipo: TipoUsuario;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface EmpresaCadastroRequest {
  razaoSocial: string;
  cnpj: string;
  email: string;
  senha: string;
}

export interface FornecedorCadastroRequest {
  nomeCompleto: string;
  cnpj: string;
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
  melhorOferta: number | null;
}

export interface CotacaoRequest {
  nomeServico: string;
  requisitos: string;
  categoria: CategoriaCotacao | null;
  orcamentoEstimado: number | null;
  dataLimite: string | null;
  empresaId: string;
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
  fornecedorId: string;
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
}

export interface Mensagem {
  id: string;
  mensagem: string;
  valorOfertado: number | null;
  tipoRemetente: TipoUsuario;
  remetenteId: string;
  remetenteNome: string;
  dataEnvio: string;
  negociacaoId: string;
}

export interface MensagemRequest {
  negociacaoId: string;
  mensagem: string | null;
  valorOfertado: number | null;
  tipoRemetente: TipoUsuario;
  remetenteId: string;
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
  email: string;
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
