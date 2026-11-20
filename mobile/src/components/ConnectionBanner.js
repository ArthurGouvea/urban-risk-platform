import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { CORES } from '../constants';
import { tempoRelativo } from '../utils/alertas';
import { useAgora } from './useAgora';

/**
 * Faixa de status da conexão exibida no topo de todas as telas.
 * Online: mostra há quanto tempo foi a última atualização e a latência (indicador "tempo de atualização").
 * Offline: avisa a queda, informa de quando são os dados exibidos e segue tentando reconectar.
 */
export function ConnectionBanner() {
  const {
    conexao, ultimaSincronizacao, latenciaMs, desconectadoDesde, reconectadoEm, tentativas, filaRelatos, sincronizarAgora,
  } = useRisk();
  const agora = useAgora(1000);

  if (conexao === 'conectando') {
    return (
      <View style={[s.banner, { backgroundColor: CORES.reconectando }]} accessibilityLiveRegion="polite">
        <Text style={s.texto}>⏳ Conectando ao servidor...</Text>
      </View>
    );
  }

  if (conexao === 'offline') {
    const horario = ultimaSincronizacao ? ultimaSincronizacao.toLocaleTimeString() : null;
    return (
      <View style={[s.banner, { backgroundColor: CORES.offline }]} accessibilityRole="alert" accessibilityLiveRegion="assertive">
        <Text style={s.texto}>⚠️ SEM CONEXÃO COM O SERVIDOR {desconectadoDesde ? `(${tempoRelativo(desconectadoDesde, agora)})` : ''}</Text>
        <Text style={s.detalhe}>
          {horario ? `Exibindo dados salvos às ${horario}.` : 'Sem dados salvos ainda.'} Reconectando automaticamente
          {tentativas ? ` • tentativa ${tentativas}` : ''}
          {filaRelatos.length ? ` • ${filaRelatos.length} relato(s) na fila` : ''}
        </Text>
        <TouchableOpacity
          onPress={sincronizarAgora}
          style={s.botao}
          accessibilityRole="button"
          accessibilityLabel="Tentar reconectar agora"
        >
          <Text style={s.botaoTexto}>Tentar agora</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const recemReconectado = reconectadoEm && agora - reconectadoEm < 6000;
  if (recemReconectado) {
    return (
      <View style={[s.banner, { backgroundColor: CORES.sucesso }]} accessibilityLiveRegion="polite">
        <Text style={s.texto}>✅ Reconectado! Dados atualizados em tempo real novamente.</Text>
      </View>
    );
  }

  return (
    <View style={[s.banner, { backgroundColor: CORES.online }]} accessibilityLiveRegion="polite">
      <Text style={s.texto}>
        ● Online • atualizado {tempoRelativo(ultimaSincronizacao, agora)}
        {latenciaMs != null ? ` • ${latenciaMs} ms` : ''}
      </Text>
    </View>
  );
}

const s = StyleSheet.create({
  banner: { paddingVertical: 8, paddingHorizontal: 12, alignItems: 'center' },
  texto: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 13, textAlign: 'center' },
  detalhe: { color: '#FEE2E2', fontSize: 12, marginTop: 2, textAlign: 'center' },
  botao: { marginTop: 6, backgroundColor: 'rgba(255,255,255,0.2)', paddingHorizontal: 14, paddingVertical: 6, borderRadius: 14, minHeight: 32, justifyContent: 'center' },
  botaoTexto: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 13 },
});
