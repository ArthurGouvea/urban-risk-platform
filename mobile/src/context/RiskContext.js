import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as api from '../services/api';
import { dispararNotificacao, prepararNotificacoes } from '../services/notificacoes';
import { API_URL_PADRAO, CHAVES_STORAGE, INTERVALO_ATUALIZACAO_MS, INTERVALO_RECONEXAO_MS } from '../config';
import { detectarNotificacoes, gerarIdDispositivo } from '../utils/alertas';

/**
 * Estado global do app: dados em tempo real, resiliência offline e notificações.
 *
 * Ciclo de sincronização (setTimeout encadeado, nunca sobrepõe requisições):
 *   online  → busca a cada 3 s; mede latência; salva cache local; detecta alertas novos/escalados.
 *   falha   → marca OFFLINE (timeout de 2,5 s), mantém os últimos dados do cache e continua
 *             tentando a cada 3 s, sem bloquear nenhuma tela.
 *   volta   → mostra "Reconectado", atualiza os dados e envia os relatos que ficaram na fila offline.
 */
const RiskContext = createContext(null);

const PREFERENCIAS_PADRAO = {
  perfil: 'Morador',
  tokenAgente: '',
  lgpdAceito: false,
  notificacoes: true,
  apiUrl: API_URL_PADRAO,
};

// versões antigas salvavam "Agente Local"
const migrarPerfil = (p) => (p === 'Agente Local' ? 'Agente da Defesa Civil' : p);

async function lerJson(chave, padrao) {
  try {
    const bruto = await AsyncStorage.getItem(chave);
    return bruto ? JSON.parse(bruto) : padrao;
  } catch (e) {
    return padrao;
  }
}

function salvarJson(chave, valor) {
  return AsyncStorage.setItem(chave, JSON.stringify(valor)).catch(() => {});
}

export function RiskProvider({ children }) {
  const [pronto, setPronto] = useState(false);
  const [preferencias, setPreferencias] = useState(PREFERENCIAS_PADRAO);
  const [dispositivoId, setDispositivoId] = useState(null);

  const [alertas, setAlertas] = useState([]);
  const [leituras, setLeituras] = useState([]);
  const [conexao, setConexao] = useState('conectando'); // conectando | online | offline
  const [ultimaSincronizacao, setUltimaSincronizacao] = useState(null);
  const [latenciaMs, setLatenciaMs] = useState(null);
  const [desconectadoDesde, setDesconectadoDesde] = useState(null);
  const [reconectadoEm, setReconectadoEm] = useState(null);
  const [tentativas, setTentativas] = useState(0);
  const [filaRelatos, setFilaRelatos] = useState([]);

  const [avisos, setAvisos] = useState([]);
  const [naoLidos, setNaoLidos] = useState(0);
  const [toast, setToast] = useState(null);

  const conexaoRef = useRef('conectando');
  const preferenciasRef = useRef(PREFERENCIAS_PADRAO);
  const filaRef = useRef([]);
  const mapaNiveisRef = useRef(null);
  const enviandoFilaRef = useRef(false);

  // ---------- inicialização: preferências, id do dispositivo e cache offline ----------
  useEffect(() => {
    (async () => {
      const prefs = { ...PREFERENCIAS_PADRAO, ...(await lerJson(CHAVES_STORAGE.preferencias, {})) };
      prefs.perfil = migrarPerfil(prefs.perfil);
      preferenciasRef.current = prefs;
      setPreferencias(prefs);
      api.definirBaseUrl(prefs.apiUrl || API_URL_PADRAO);

      let id = await AsyncStorage.getItem(CHAVES_STORAGE.dispositivo).catch(() => null);
      if (!id) {
        id = gerarIdDispositivo();
        AsyncStorage.setItem(CHAVES_STORAGE.dispositivo, id).catch(() => {});
      }
      setDispositivoId(id);

      const cache = await lerJson(CHAVES_STORAGE.cacheAlertas, []);
      const meta = await lerJson(CHAVES_STORAGE.cacheMeta, null);
      if (cache.length) setAlertas(cache);
      if (meta?.sincronizadoEm) setUltimaSincronizacao(new Date(meta.sincronizadoEm));

      const fila = await lerJson(CHAVES_STORAGE.filaRelatos, []);
      filaRef.current = fila;
      setFilaRelatos(fila);

      if (prefs.notificacoes) prepararNotificacoes();
      setPronto(true);
    })();
  }, []);

  const atualizarConexao = useCallback((estado) => {
    conexaoRef.current = estado;
    setConexao(estado);
  }, []);

  const mostrarToast = useCallback((t) => setToast({ ...t, id: Date.now() }), []);

  // ---------- notificações de alertas novos / escalados ----------
  const processarNotificacoes = useCallback((lista) => {
    const { mapa, notificar } = detectarNotificacoes(mapaNiveisRef.current, lista);
    mapaNiveisRef.current = mapa;
    if (!notificar.length) return;
    const agora = new Date();
    const itens = notificar.slice(0, 5).map((n) => ({ ...n, recebidoEm: agora.toISOString() }));
    setAvisos((anteriores) => [...itens, ...anteriores].slice(0, 50));
    setNaoLidos((n) => n + itens.length);
    mostrarToast({ tipo: 'alerta', item: itens[0], extras: itens.length - 1 });
    itens.forEach((item) => dispararNotificacao(item, { sistema: preferenciasRef.current.notificacoes }));
  }, [mostrarToast]);

  // ---------- fila offline de relatos ----------
  const persistirFila = useCallback((fila) => {
    filaRef.current = fila;
    setFilaRelatos(fila);
    salvarJson(CHAVES_STORAGE.filaRelatos, fila);
  }, []);

  const enviarFila = useCallback(async () => {
    if (enviandoFilaRef.current || !filaRef.current.length) return;
    enviandoFilaRef.current = true;
    const restantes = [];
    let enviados = 0;
    for (const relato of filaRef.current) {
      try {
        await api.enviarRelato(relato, relato.origemDado === 'BASE_PUBLICA' ? preferenciasRef.current.tokenAgente : undefined);
        enviados += 1;
      } catch (e) {
        // erro de validação: descarta; erro de rede: mantém para a próxima tentativa
        if (!e?.response) restantes.push(relato);
      }
    }
    persistirFila(restantes);
    enviandoFilaRef.current = false;
    if (enviados) mostrarToast({ tipo: 'info', texto: `📤 ${enviados} relato(s) guardado(s) offline foram enviados.` });
  }, [persistirFila, mostrarToast]);

  // ---------- sincronização em tempo real ----------
  const sincronizarAgora = useCallback(async () => {
    try {
      const r = await api.sincronizar();
      const estavaOffline = conexaoRef.current === 'offline';
      setAlertas(r.alertas);
      setLeituras(r.leituras);
      setLatenciaMs(r.latenciaMs);
      const agora = new Date();
      setUltimaSincronizacao(agora);
      setTentativas(0);
      atualizarConexao('online');
      if (estavaOffline) {
        setReconectadoEm(agora);
        setDesconectadoDesde(null);
      }
      processarNotificacoes(r.alertas);
      salvarJson(CHAVES_STORAGE.cacheAlertas, r.alertas);
      salvarJson(CHAVES_STORAGE.cacheMeta, { sincronizadoEm: agora.toISOString() });
      if (filaRef.current.length) enviarFila();
      return true;
    } catch (e) {
      if (conexaoRef.current !== 'offline') setDesconectadoDesde(new Date());
      atualizarConexao('offline');
      setTentativas((t) => t + 1);
      return false;
    }
  }, [atualizarConexao, processarNotificacoes, enviarFila]);

  useEffect(() => {
    if (!pronto) return undefined;
    let ativo = true;
    let timer;
    const ciclo = async () => {
      const ok = await sincronizarAgora();
      if (ativo) timer = setTimeout(ciclo, ok ? INTERVALO_ATUALIZACAO_MS : INTERVALO_RECONEXAO_MS);
    };
    ciclo();
    return () => {
      ativo = false;
      clearTimeout(timer);
    };
  }, [pronto, sincronizarAgora]);

  // ---------- ações expostas às telas ----------
  const atualizarPreferencias = useCallback((parcial) => {
    const novas = { ...preferenciasRef.current, ...parcial };
    preferenciasRef.current = novas;
    setPreferencias(novas);
    salvarJson(CHAVES_STORAGE.preferencias, novas);
    if (parcial.apiUrl) {
      api.definirBaseUrl(parcial.apiUrl);
      mapaNiveisRef.current = null; // outro servidor: não notificar tudo como "novo"
    }
    if (parcial.notificacoes) prepararNotificacoes();
  }, []);

  /**
   * Envia o relato (ou boletim oficial, se dados.origemDado = BASE_PUBLICA);
   * se o servidor estiver fora, guarda na fila e envia sozinho ao reconectar.
   */
  const registrarRelato = useCallback(async (dados) => {
    const oficial = dados.origemDado === 'BASE_PUBLICA';
    const relato = { ...dados, origemDado: oficial ? 'BASE_PUBLICA' : 'REGISTRO_COMUNITARIO', dispositivoId: oficial ? undefined : dispositivoId };
    if (conexaoRef.current === 'online') {
      try {
        const alerta = await api.enviarRelato(relato, oficial ? preferenciasRef.current.tokenAgente : undefined);
        sincronizarAgora();
        return { status: 'enviado', alerta };
      } catch (e) {
        if (e?.response) throw new Error(api.mensagemErro(e));
      }
    }
    persistirFila([...filaRef.current, { ...relato, criadoEm: new Date().toISOString() }]);
    return { status: 'fila' };
  }, [dispositivoId, persistirFila, sincronizarAgora]);

  /** Ação de agente/operador sobre o ciclo de vida do evento (exige conexão e código de acesso). */
  const executarAcaoAgente = useCallback(async (id, acao, motivo) => {
    if (conexaoRef.current !== 'online') throw new Error('Sem conexão: ações de agente precisam do servidor.');
    try {
      const { perfil, tokenAgente } = preferenciasRef.current;
      const alerta = await api.executarAcao(id, acao, motivo, perfil, tokenAgente);
      sincronizarAgora();
      return alerta;
    } catch (e) {
      throw new Error(api.mensagemErro(e));
    }
  }, [sincronizarAgora]);

  const marcarAvisosComoLidos = useCallback(() => setNaoLidos(0), []);
  const fecharToast = useCallback(() => setToast(null), []);

  /** Revoga o consentimento LGPD: apaga todos os dados locais e gera um novo identificador. */
  const revogarConsentimento = useCallback(async () => {
    await AsyncStorage.multiRemove(Object.values(CHAVES_STORAGE)).catch(() => {});
    const novoId = gerarIdDispositivo();
    AsyncStorage.setItem(CHAVES_STORAGE.dispositivo, novoId).catch(() => {});
    setDispositivoId(novoId);
    persistirFila([]);
    setAvisos([]);
    setNaoLidos(0);
    const prefs = { ...PREFERENCIAS_PADRAO, apiUrl: preferenciasRef.current.apiUrl };
    preferenciasRef.current = prefs;
    setPreferencias(prefs);
    salvarJson(CHAVES_STORAGE.preferencias, prefs);
  }, [persistirFila]);

  const valor = useMemo(() => ({
    pronto, preferencias, dispositivoId,
    alertas, leituras, conexao, ultimaSincronizacao, latenciaMs, desconectadoDesde, reconectadoEm, tentativas,
    filaRelatos, avisos, naoLidos, toast,
    sincronizarAgora, atualizarPreferencias, registrarRelato, executarAcaoAgente, marcarAvisosComoLidos, fecharToast, revogarConsentimento,
  }), [pronto, preferencias, dispositivoId, alertas, leituras, conexao, ultimaSincronizacao, latenciaMs,
    desconectadoDesde, reconectadoEm, tentativas, filaRelatos, avisos, naoLidos, toast,
    sincronizarAgora, atualizarPreferencias, registrarRelato, executarAcaoAgente, marcarAvisosComoLidos, fecharToast, revogarConsentimento]);

  return <RiskContext.Provider value={valor}>{children}</RiskContext.Provider>;
}

export function useRisk() {
  const ctx = useContext(RiskContext);
  if (!ctx) throw new Error('useRisk deve ser usado dentro de <RiskProvider>');
  return ctx;
}
