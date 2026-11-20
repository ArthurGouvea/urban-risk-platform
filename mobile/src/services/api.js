import axios from 'axios';
import { API_URL_PADRAO, TIMEOUT_MS } from '../config';

// Camada de acesso à API REST do backend Spring Boot.
const cliente = axios.create({ baseURL: API_URL_PADRAO, timeout: TIMEOUT_MS });

export function definirBaseUrl(url) {
  cliente.defaults.baseURL = url.replace(/\/+$/, '');
}

export function obterBaseUrl() {
  return cliente.defaults.baseURL;
}

/** Busca alertas priorizados + leituras de sensores e mede a latência total (indicador "tempo de atualização"). */
export async function sincronizar() {
  const inicio = Date.now();
  const [alertas, leituras] = await Promise.all([
    cliente.get('/alertas'),
    cliente.get('/sensores/leituras/recentes').catch(() => ({ data: [] })),
  ]);
  return { alertas: alertas.data, leituras: leituras.data, latenciaMs: Date.now() - inicio };
}

export async function verificarSaude(url) {
  const base = (url || obterBaseUrl()).replace(/\/+$/, '');
  const inicio = Date.now();
  const r = await axios.get(`${base}/health`, { timeout: TIMEOUT_MS });
  return { ...r.data, latenciaMs: Date.now() - inicio };
}

const cabecalhoAgente = (token) => (token ? { headers: { 'X-Token-Agente': token } } : undefined);

/** Relato comunitário ou, com token de operador, boletim oficial (BASE_PUBLICA). */
export async function enviarRelato(relato, token) {
  const r = await cliente.post('/alertas', relato, cabecalhoAgente(token));
  return r.data;
}

/** Linha do tempo (transições de estado) de um evento. */
export async function buscarHistorico(id) {
  const r = await cliente.get(`/alertas/${id}/historico`);
  return r.data;
}

/** Ação de agente/operador: CONFIRMAR, DESCARTAR, EMITIR_ALERTA, MONITORAR ou ENCERRAR. */
export async function executarAcao(id, acao, motivo, perfil, token) {
  const r = await cliente.post(`/alertas/${id}/acoes`, { acao, motivo, perfil }, cabecalhoAgente(token));
  return r.data;
}

export async function analisarTexto(texto, categoria) {
  const r = await cliente.post('/ia/analisar-texto', { texto, categoria });
  return r.data;
}

export async function buscarModeloIa() {
  const r = await cliente.get('/ia/modelo');
  return r.data;
}

/** Extrai uma mensagem amigável de um erro do axios. */
export function mensagemErro(erro) {
  const dados = erro?.response?.data;
  if (dados?.campos) return Object.values(dados.campos).join('\n');
  if (dados?.erro) return dados.erro;
  if (erro?.response?.status === 401) return 'Código de acesso de agente/operador inválido. Verifique na aba Perfil.';
  if (erro?.code === 'ECONNABORTED') return 'Tempo de resposta esgotado.';
  return 'Servidor indisponível.';
}
