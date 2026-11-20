import { Platform } from 'react-native';
import Constants from 'expo-constants';

/**
 * Descobre a URL da API automaticamente:
 *  1. EXPO_PUBLIC_API_URL (arquivo .env) — se definida, tem prioridade;
 *  2. Expo Go no celular: usa o IP do computador que roda o Metro (mesma rede Wi-Fi);
 *  3. Emulador Android: 10.0.2.2 aponta para o localhost do computador;
 *  4. Web / simulador iOS: localhost.
 * A URL também pode ser alterada em tempo de execução na aba Perfil.
 */
function detectarUrlPadrao() {
  if (process.env.EXPO_PUBLIC_API_URL) return process.env.EXPO_PUBLIC_API_URL;
  if (Platform.OS === 'web') return 'http://localhost:8080/api';

  const hostUri = Constants.expoConfig?.hostUri; // ex.: "192.168.0.15:8081"
  const host = hostUri ? hostUri.split(':')[0] : null;
  if (host && host !== 'localhost' && host !== '127.0.0.1') return `http://${host}:8080/api`;

  return Platform.OS === 'android' ? 'http://10.0.2.2:8080/api' : 'http://localhost:8080/api';
}

export const API_URL_PADRAO = detectarUrlPadrao();

/** Frequência de atualização em tempo real (online) e de tentativa de reconexão (offline). */
export const INTERVALO_ATUALIZACAO_MS = 3000;
export const INTERVALO_RECONEXAO_MS = 3000;
/** Timeout curto: a queda do servidor é percebida em ~2,5 s, sem travar a interface. */
export const TIMEOUT_MS = 2500;

export const CHAVES_STORAGE = {
  cacheAlertas: '@urban_risk/alertas_cache',
  cacheMeta: '@urban_risk/alertas_meta',
  filaRelatos: '@urban_risk/fila_relatos',
  preferencias: '@urban_risk/preferencias',
  dispositivo: '@urban_risk/dispositivo_id',
};
