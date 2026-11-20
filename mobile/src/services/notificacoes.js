import { Platform, Vibration } from 'react-native';
import { CATEGORIAS, NIVEIS, rotulo } from '../constants';

// Notificações locais do sistema (Android/iOS). Na web, o app usa apenas o aviso visual interno.
// O módulo é carregado sob demanda: no Expo Go ele emite um aviso sobre *push remoto*
// (que não usamos) — notificações LOCAIS continuam funcionando normalmente.
const suportado = Platform.OS !== 'web';
let modulo = null;

function notificacoes() {
  if (!suportado) return null;
  if (!modulo) {
    try {
      modulo = require('expo-notifications');
      modulo.setNotificationHandler({
        handleNotification: async () => ({
          shouldShowBanner: true,
          shouldShowList: true,
          shouldPlaySound: true,
          shouldSetBadge: false,
        }),
      });
    } catch (e) {
      modulo = null;
    }
  }
  return modulo;
}

export async function prepararNotificacoes() {
  const N = notificacoes();
  if (!N) return false;
  try {
    if (Platform.OS === 'android') {
      await N.setNotificationChannelAsync('alertas-risco', {
        name: 'Alertas de risco',
        importance: N.AndroidImportance.MAX,
        vibrationPattern: [0, 400, 200, 400],
      });
    }
    const atual = await N.getPermissionsAsync();
    if (atual.granted) return true;
    const pedido = await N.requestPermissionsAsync();
    return pedido.granted;
  } catch (e) {
    return false; // nunca deixa a falha de notificação travar o app
  }
}

export function textoNotificacao({ alerta, motivo }) {
  const nivel = rotulo(NIVEIS, alerta.nivelRisco).toUpperCase();
  const cat = CATEGORIAS[alerta.categoria];
  const titulo = motivo === 'ESCALOU'
    ? `⬆️ Risco escalou para ${nivel} • ${alerta.bairro}`
    : `${cat ? cat.icone : '⚠️'} Alerta ${nivel} • ${alerta.bairro}`;
  return { titulo, corpo: alerta.titulo };
}

export async function dispararNotificacao(item, { sistema = true } = {}) {
  if (!suportado) return;
  try {
    Vibration.vibrate(item.alerta.nivelRisco === 'CRITICO' ? [0, 400, 200, 400] : 300);
    const N = sistema ? notificacoes() : null;
    if (!N) return;
    const { titulo, corpo } = textoNotificacao(item);
    await N.scheduleNotificationAsync({
      content: { title: titulo, body: corpo, data: { alertaId: item.alerta.id } },
      trigger: null,
    });
  } catch (e) {
    // silencioso: o aviso visual dentro do app continua funcionando
  }
}
