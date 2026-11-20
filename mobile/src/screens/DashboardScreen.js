import React, { useMemo } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { AlertaCard } from '../components/AlertaCard';
import { MapaRiscos } from '../components/MapaRiscos';
import { estilos } from '../components/estilos';
import { CATEGORIAS, CORES, NIVEIS, TIPOS_SENSOR, perfilPrivilegiado } from '../constants';
import { agruparPorBairro, contarPorNivel } from '../utils/alertas';

export default function DashboardScreen({ navigation }) {
  const { alertas, leituras, preferencias, conexao, sincronizarAgora } = useRisk();
  const contagem = useMemo(() => contarPorNivel(alertas), [alertas]);
  const zonas = useMemo(() => agruparPorBairro(alertas).slice(0, 5), [alertas]);
  const sensores = useMemo(() => {
    const vistos = new Map();
    for (const l of leituras) if (!vistos.has(l.sensorId)) vistos.set(l.sensorId, l);
    return [...vistos.values()];
  }, [leituras]);
  const emValidacao = alertas.filter((a) => a.status === 'EM_VALIDACAO').length;
  const agente = perfilPrivilegiado(preferencias.perfil);

  return (
    <Tela>
      <ScrollView
        contentContainerStyle={estilos.scroll}
        refreshControl={<RefreshControl refreshing={false} onRefresh={sincronizarAgora} tintColor={CORES.destaque} />}
      >
        <Text style={estilos.cabecalho} accessibilityRole="header">Painel • {preferencias.perfil}</Text>
        <Text style={estilos.subtitulo}>{alertas.length} alerta(s) ativo(s), ordenados por prioridade da IA</Text>

        {conexao === 'conectando' && !alertas.length ? (
          <ActivityIndicator size="large" color={CORES.destaque} />
        ) : (
          <>
            <View style={s.linhaStats}>
              {Object.entries(NIVEIS).map(([chave, info]) => (
                <TouchableOpacity
                  key={chave}
                  style={[s.stat, { borderTopColor: info.cor }]}
                  onPress={() => navigation.navigate('Alertas', { nivel: chave })}
                  accessibilityRole="button"
                  accessibilityLabel={`${contagem[chave]} alertas de risco ${info.label}`}
                >
                  <Text style={[s.statNumero, { color: info.cor }]}>{contagem[chave]}</Text>
                  <Text style={s.statRotulo}>{info.label}</Text>
                </TouchableOpacity>
              ))}
            </View>

            {agente && (
              <TouchableOpacity style={[estilos.cartao, { borderLeftWidth: 5, borderLeftColor: CORES.destaque }]} onPress={() => navigation.navigate('Alertas', { status: 'EM_VALIDACAO' })}>
                <Text style={estilos.cartaoTitulo}>🗂️ Fila de validação ({preferencias.perfil})</Text>
                <Text style={estilos.corpo}>{emValidacao} relato(s) comunitário(s) com confiança abaixo de 60% aguardando checagem em campo.</Text>
              </TouchableOpacity>
            )}

            <View style={estilos.entreLados}>
              <Text style={estilos.secao}>🗺️ Mapa de riscos</Text>
              <TouchableOpacity onPress={() => navigation.navigate('Mapa')} accessibilityRole="button">
                <Text style={estilos.botaoSecundarioTexto}>Ampliar ›</Text>
              </TouchableOpacity>
            </View>
            <MapaRiscos alertas={alertas} altura={180} compacto onSelecionar={(a) => navigation.navigate('DetalheAlerta', { id: a.id, alerta: a })} />

            <Text style={estilos.secao}>🔥 Prioridades agora</Text>
            {alertas.slice(0, 3).map((a) => (
              <AlertaCard key={a.id} alerta={a} compacto onPress={() => navigation.navigate('DetalheAlerta', { id: a.id, alerta: a })} />
            ))}
            {!alertas.length && <Text style={estilos.vazio}>Nenhum alerta ativo no momento.</Text>}

            <Text style={estilos.secao}>📡 Sensores em tempo real</Text>
            {sensores.length ? (
              <View style={estilos.cartao}>
                {sensores.map((l) => (
                  <View key={l.sensorId} style={[estilos.entreLados, s.linhaSensor]} accessible accessibilityLabel={`${TIPOS_SENSOR[l.tipo] || l.tipo} em ${l.bairro}: ${l.valor} ${l.unidade}${l.anomalia ? ', anomalia detectada' : ''}`}>
                    <Text style={[estilos.corpo, { flex: 1 }]}>{l.anomalia ? '🔴' : '🟢'} {TIPOS_SENSOR[l.tipo] || l.tipo} • {l.bairro}</Text>
                    <Text style={[estilos.corpo, { fontWeight: 'bold', color: l.anomalia ? '#FCA5A5' : CORES.texto }]}>{Number(l.valor).toFixed(1)} {l.unidade}</Text>
                  </View>
                ))}
                <Text style={estilos.meta}>🔴 = anomalia detectada pela IA (z-score/limiar)</Text>
              </View>
            ) : (
              <Text style={estilos.meta}>Aguardando leituras dos sensores...</Text>
            )}

            <Text style={estilos.secao}>📍 Zonas monitoradas</Text>
            {zonas.map((z) => (
              <TouchableOpacity
                key={z.bairro}
                style={[estilos.cartao, { borderLeftWidth: 5, borderLeftColor: (NIVEIS[z.nivel] || {}).cor || CORES.borda }]}
                onPress={() => navigation.navigate('Alertas', { bairro: z.bairro })}
                accessibilityRole="button"
                accessibilityLabel={`${z.bairro}: ${z.total} alerta(s), maior risco ${(NIVEIS[z.nivel] || {}).label}`}
              >
                <View style={estilos.entreLados}>
                  <Text style={estilos.cartaoTitulo}>{z.bairro}</Text>
                  <Text style={[estilos.corpo, { color: (NIVEIS[z.nivel] || {}).cor, fontWeight: 'bold' }]}>{(NIVEIS[z.nivel] || {}).label} • {z.maiorScore}</Text>
                </View>
                <Text style={estilos.meta}>{z.total} alerta(s) • {z.categorias.map((c) => (CATEGORIAS[c] || {}).icone || c).join(' ')}</Text>
              </TouchableOpacity>
            ))}

            <TouchableOpacity style={estilos.botaoSecundario} onPress={() => navigation.navigate('Transparencia')} accessibilityRole="button">
              <Text style={estilos.botaoSecundarioTexto}>Como a IA prioriza os alertas? (fontes e modelo)</Text>
            </TouchableOpacity>
          </>
        )}
      </ScrollView>
    </Tela>
  );
}

const s = StyleSheet.create({
  linhaStats: { flexDirection: 'row', marginHorizontal: -4, marginBottom: 8 },
  stat: { flex: 1, backgroundColor: CORES.cartao, paddingVertical: 12, borderRadius: 10, marginHorizontal: 4, borderTopWidth: 4, alignItems: 'center', minHeight: 64 },
  statNumero: { fontSize: 24, fontWeight: 'bold' },
  statRotulo: { color: CORES.textoSecundario, fontSize: 12, marginTop: 2 },
  linhaSensor: { paddingVertical: 6, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: CORES.borda },
});
