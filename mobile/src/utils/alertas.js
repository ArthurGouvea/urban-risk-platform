// Funções puras (sem React Native) — testadas com `npm test` (node:test).

const NIVEIS_NOTIFICAVEIS = ['CRITICO', 'ALTO'];

/** Converte datas do backend (ISO "2026-11-19T14:05:00.123" ou array [a,m,d,h,min,s,ns]) em Date. */
export function parseData(valor) {
  if (!valor) return null;
  if (Array.isArray(valor)) {
    const [a, m, d, h = 0, min = 0, s = 0, ns = 0] = valor;
    return new Date(a, m - 1, d, h, min, s, Math.floor(ns / 1e6));
  }
  const data = new Date(valor);
  return Number.isNaN(data.getTime()) ? null : data;
}

/** "agora", "há 15 s", "há 4 min", "há 2 h", "há 3 d". */
export function tempoRelativo(data, agora = new Date()) {
  const d = data instanceof Date ? data : parseData(data);
  if (!d) return '—';
  const seg = Math.max(0, Math.round((agora.getTime() - d.getTime()) / 1000));
  if (seg < 5) return 'agora';
  if (seg < 60) return `há ${seg} s`;
  const min = Math.floor(seg / 60);
  if (min < 60) return `há ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `há ${h} h`;
  return `há ${Math.floor(h / 24)} d`;
}

/**
 * Compara a lista nova com o estado anterior e decide o que notificar:
 *  - alerta NOVO com nível CRITICO ou ALTO;
 *  - alerta existente que ESCALOU para CRITICO.
 * Na primeira carga (anterior = null) nada é notificado, só memorizado.
 */
export function detectarNotificacoes(anterior, alertas) {
  const mapa = new Map();
  const notificar = [];
  for (const a of alertas || []) {
    mapa.set(a.id, a.nivelRisco);
    if (!anterior) continue;
    const nivelAnterior = anterior.get(a.id);
    if (nivelAnterior === undefined && NIVEIS_NOTIFICAVEIS.includes(a.nivelRisco)) {
      notificar.push({ alerta: a, motivo: 'NOVO' });
    } else if (nivelAnterior && nivelAnterior !== 'CRITICO' && a.nivelRisco === 'CRITICO') {
      notificar.push({ alerta: a, motivo: 'ESCALOU' });
    }
  }
  return { mapa, notificar };
}

export function contarPorNivel(alertas) {
  const contagem = { CRITICO: 0, ALTO: 0, MODERADO: 0, BAIXO: 0 };
  for (const a of alertas || []) {
    if (contagem[a.nivelRisco] !== undefined) contagem[a.nivelRisco] += 1;
  }
  return contagem;
}

/** Zonas monitoradas: agrupa por bairro, com o maior score de cada um (ordem decrescente). */
export function agruparPorBairro(alertas) {
  const zonas = new Map();
  for (const a of alertas || []) {
    const chave = a.bairro || 'Sem bairro';
    const z = zonas.get(chave) || { bairro: chave, total: 0, maiorScore: -1, nivel: 'BAIXO', categorias: new Set() };
    z.total += 1;
    z.categorias.add(a.categoria);
    if ((a.scorePrioridade ?? 0) > z.maiorScore) {
      z.maiorScore = a.scorePrioridade ?? 0;
      z.nivel = a.nivelRisco;
    }
    zonas.set(chave, z);
  }
  return [...zonas.values()]
    .map((z) => ({ ...z, categorias: [...z.categorias] }))
    .sort((x, y) => y.maiorScore - x.maiorScore);
}

/** Gera um identificador aleatório de instalação (não é dado pessoal; é pseudonimizado no servidor). */
export function gerarIdDispositivo() {
  const aleatorio = () => Math.random().toString(36).slice(2, 10);
  return `app-${Date.now().toString(36)}-${aleatorio()}${aleatorio()}`;
}
