import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { CATEGORIAS, CORES, NIVEIS, ORIGENS, STATUS, rotulo } from '../constants';
import { tempoRelativo } from '../utils/alertas';
import { estilos } from './estilos';

export function NivelBadge({ nivel, score }) {
  const info = NIVEIS[nivel] || { label: nivel, cor: CORES.textoFraco };
  return (
    <View style={[s.badge, { backgroundColor: info.cor }]}>
      <Text style={s.badgeTexto}>{info.label.toUpperCase()}{score != null ? ` • ${score}` : ''}</Text>
    </View>
  );
}

export function AlertaCard({ alerta, onPress, compacto = false }) {
  const nivel = NIVEIS[alerta.nivelRisco] || { cor: CORES.textoFraco, label: alerta.nivelRisco };
  const cat = CATEGORIAS[alerta.categoria] || { label: alerta.categoria, icone: '⚠️' };
  const origem = ORIGENS[alerta.origemDado] || { label: alerta.origemDado, icone: '•' };
  const descricaoAcessivel = `Risco ${nivel.label}, prioridade ${alerta.scorePrioridade} de 100. ${cat.label} em ${alerta.bairro}. ${alerta.titulo}. Fonte ${origem.label}, confiança ${alerta.percentualConfianca} por cento.`;

  return (
    <TouchableOpacity
      style={[estilos.cartao, s.cartao, { borderLeftColor: nivel.cor }]}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={descricaoAcessivel}
      accessibilityHint="Abre os detalhes e a explicação da IA"
    >
      <View style={estilos.entreLados}>
        <NivelBadge nivel={alerta.nivelRisco} score={alerta.scorePrioridade} />
        <Text style={estilos.meta}>{tempoRelativo(alerta.atualizadoEm || alerta.dataHora)}</Text>
      </View>
      <Text style={estilos.cartaoTitulo}>{cat.icone} {alerta.titulo}</Text>
      {!compacto && <Text style={estilos.corpo} numberOfLines={2}>{alerta.descricao}</Text>}
      <Text style={estilos.meta}>
        📍 {alerta.bairro} • {origem.icone} {origem.label} • Confiança {alerta.percentualConfianca}% ({rotulo({ ALTA: 'alta', MEDIA: 'média', BAIXA: 'baixa' }, alerta.nivelConfianca)})
      </Text>
      {!compacto && <Text style={estilos.meta}>Status: {STATUS[alerta.status] || alerta.status}</Text>}
    </TouchableOpacity>
  );
}

const s = StyleSheet.create({
  cartao: { borderLeftWidth: 5 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  badgeTexto: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 12 },
});
