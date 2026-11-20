import React, { useCallback } from 'react';
import { ScrollView, Text } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { AlertaCard } from '../components/AlertaCard';
import { estilos } from '../components/estilos';
import { textoNotificacao } from '../services/notificacoes';
import { tempoRelativo } from '../utils/alertas';
import { useAgora } from '../components/useAgora';

export default function NotificacoesScreen({ navigation }) {
  const { alertas, avisos, marcarAvisosComoLidos } = useRisk();
  const agora = useAgora(5000);
  useFocusEffect(useCallback(() => { marcarAvisosComoLidos(); }, [marcarAvisosComoLidos]));
  const urgentes = alertas.filter((a) => a.scorePrioridade >= 70);

  return (
    <Tela>
      <ScrollView contentContainerStyle={estilos.scroll}>
        <Text style={estilos.cabecalho} accessibilityRole="header">Avisos</Text>
        <Text style={estilos.subtitulo}>Você é avisado quando surge um alerta ALTO/CRÍTICO ou quando um risco escala para CRÍTICO.</Text>

        <Text style={estilos.secao}>🔔 Recebidos nesta sessão ({avisos.length})</Text>
        {avisos.length === 0 && <Text style={estilos.meta}>Nenhum aviso novo desde que você abriu o app.</Text>}
        {avisos.map((item, i) => {
          const { titulo } = textoNotificacao(item);
          return (
            <Text
              key={`${item.alerta.id}-${item.recebidoEm}-${i}`}
              style={[estilos.cartao, estilos.corpo]}
              onPress={() => navigation.navigate('DetalheAlerta', { id: item.alerta.id, alerta: item.alerta })}
              accessibilityRole="button"
            >
              {titulo}{'\n'}<Text style={estilos.meta}>{item.alerta.titulo} • {tempoRelativo(item.recebidoEm, agora)}</Text>
            </Text>
          );
        })}

        <Text style={estilos.secao}>🚨 Alertas urgentes ativos ({urgentes.length})</Text>
        {urgentes.map((a) => (
          <AlertaCard key={a.id} alerta={a} compacto onPress={() => navigation.navigate('DetalheAlerta', { id: a.id, alerta: a })} />
        ))}
        {urgentes.length === 0 && <Text style={estilos.vazio}>Nenhum alerta de prioridade alta no momento.</Text>}
      </ScrollView>
    </Tela>
  );
}
