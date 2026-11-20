import React, { useMemo, useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { CATEGORIAS, CORES, NIVEIS } from '../constants';

// Limites padrão (centro expandido de São Paulo) quando há poucos pontos.
const LIMITES_PADRAO = { latMin: -23.60, latMax: -23.53, lonMin: -46.72, lonMax: -46.58 };

/**
 * Mapa esquemático de riscos: posiciona cada evento pelas coordenadas (lat/lon) dentro de um
 * retângulo, sem depender de serviço de mapas externo (funciona offline e na web).
 * Cor = nível de risco, tamanho = prioridade. Eventos sem coordenadas não aparecem.
 */
export function MapaRiscos({ alertas, altura = 260, onSelecionar, compacto = false }) {
  const [largura, setLargura] = useState(0);
  const pontos = useMemo(() => (alertas || []).filter((a) => a.latitude != null && a.longitude != null), [alertas]);

  const limites = useMemo(() => {
    if (pontos.length < 2) return LIMITES_PADRAO;
    const lats = pontos.map((p) => p.latitude);
    const lons = pontos.map((p) => p.longitude);
    const margemLat = Math.max(0.006, (Math.max(...lats) - Math.min(...lats)) * 0.15);
    const margemLon = Math.max(0.006, (Math.max(...lons) - Math.min(...lons)) * 0.15);
    return {
      latMin: Math.min(...lats) - margemLat, latMax: Math.max(...lats) + margemLat,
      lonMin: Math.min(...lons) - margemLon, lonMax: Math.max(...lons) + margemLon,
    };
  }, [pontos]);

  const posicionar = (a) => ({
    x: ((a.longitude - limites.lonMin) / (limites.lonMax - limites.lonMin)) * largura,
    y: ((limites.latMax - a.latitude) / (limites.latMax - limites.latMin)) * altura,
  });

  // pontos de menor prioridade primeiro, para os críticos ficarem por cima
  const ordenados = [...pontos].sort((a, b) => (a.scorePrioridade ?? 0) - (b.scorePrioridade ?? 0));

  return (
    <View>
      <View
        style={[s.mapa, { height: altura }]}
        onLayout={(e) => setLargura(e.nativeEvent.layout.width)}
        accessibilityLabel={`Mapa com ${pontos.length} eventos de risco`}
      >
        {[1, 2, 3].map((i) => (
          <View key={`h${i}`} style={[s.gradeH, { top: (altura / 4) * i }]} />
        ))}
        {[1, 2, 3].map((i) => (
          <View key={`v${i}`} style={[s.gradeV, { left: `${25 * i}%` }]} />
        ))}
        <Text style={s.norte}>N ↑</Text>
        {largura > 0 && ordenados.map((a) => {
          const { x, y } = posicionar(a);
          const tamanho = 14 + Math.round(((a.scorePrioridade ?? 0) / 100) * 18);
          const cor = (NIVEIS[a.nivelRisco] || {}).cor || CORES.textoFraco;
          return (
            <TouchableOpacity
              key={a.id}
              onPress={() => onSelecionar && onSelecionar(a)}
              style={[s.ponto, { left: x - tamanho / 2, top: y - tamanho / 2, width: tamanho, height: tamanho, borderRadius: tamanho / 2, backgroundColor: cor }]}
              accessibilityRole="button"
              accessibilityLabel={`${(CATEGORIAS[a.categoria] || {}).label} em ${a.bairro}, risco ${(NIVEIS[a.nivelRisco] || {}).label}`}
            >
              <Text style={s.icone}>{(CATEGORIAS[a.categoria] || {}).icone}</Text>
            </TouchableOpacity>
          );
        })}
        {largura > 0 && !compacto && ordenados.map((a) => {
          const { x, y } = posicionar(a);
          return <Text key={`l${a.id}`} style={[s.rotulo, { left: x - 40, top: y + 16 }]} numberOfLines={1}>{a.bairro}</Text>;
        })}
        {pontos.length === 0 && <Text style={s.vazio}>Nenhum evento com localização.</Text>}
      </View>
      <View style={s.legenda}>
        {Object.entries(NIVEIS).map(([k, v]) => (
          <View key={k} style={s.itemLegenda}>
            <View style={[s.bolinha, { backgroundColor: v.cor }]} />
            <Text style={s.textoLegenda}>{v.label}</Text>
          </View>
        ))}
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  mapa: { backgroundColor: '#0B2239', borderRadius: 12, overflow: 'hidden', borderWidth: 1, borderColor: CORES.borda },
  gradeH: { position: 'absolute', left: 0, right: 0, height: 1, backgroundColor: 'rgba(148,163,184,0.12)' },
  gradeV: { position: 'absolute', top: 0, bottom: 0, width: 1, backgroundColor: 'rgba(148,163,184,0.12)' },
  norte: { position: 'absolute', right: 8, top: 6, color: CORES.textoFraco, fontSize: 11, fontWeight: 'bold' },
  ponto: { position: 'absolute', alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderColor: '#FFFFFF' },
  icone: { fontSize: 10 },
  rotulo: { position: 'absolute', width: 80, textAlign: 'center', color: '#E2E8F0', fontSize: 10, fontWeight: '600' },
  vazio: { color: CORES.textoFraco, textAlign: 'center', marginTop: 40 },
  legenda: { flexDirection: 'row', justifyContent: 'center', marginTop: 8, flexWrap: 'wrap' },
  itemLegenda: { flexDirection: 'row', alignItems: 'center', marginHorizontal: 6 },
  bolinha: { width: 10, height: 10, borderRadius: 5, marginRight: 4 },
  textoLegenda: { color: CORES.textoSecundario, fontSize: 12 },
});
