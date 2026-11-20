import React, { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { estilos } from '../components/estilos';
import { CORES, CORES_STATUS, STATUS } from '../constants';
import { buscarModeloIa } from '../services/api';

const NOMES_PESOS = {
  severidade: 'Gravidade da categoria',
  confianca: 'Confiança da fonte',
  urgencia: 'Urgência no texto (NLP)',
  anomalia: 'Anomalia de sensor',
  correlacao: 'Eventos correlacionados',
  recencia: 'Recência',
};

export default function TransparenciaScreen() {
  const { conexao } = useRisk();
  const [modelo, setModelo] = useState(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    if (conexao !== 'online') { setCarregando(false); return; }
    buscarModeloIa().then(setModelo).catch(() => {}).finally(() => setCarregando(false));
  }, [conexao]);

  return (
    <Tela comCabecalho>
      <ScrollView contentContainerStyle={estilos.scroll}>
        <Text style={estilos.cabecalho} accessibilityRole="header">Fontes e inteligência artificial</Text>
        <Text style={estilos.subtitulo}>Como cada dado é validado e como a prioridade é calculada.</Text>

        <Text style={estilos.secao}>Origens dos dados</Text>
        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>📡 Sensores IoT • confiança 90–95%</Text>
          <Text style={estilos.corpo}>Pluviômetros, nível de córregos, termômetros, tensão da rede e pressão da água. A IA detecta anomalias por z-score e limiares físicos.</Text>
        </View>
        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>🏛️ Bases públicas • confiança 95%</Text>
          <Text style={estilos.corpo}>Boletins da Defesa Civil e de operadores de energia, saneamento e transporte (publicados com código de acesso).</Text>
        </View>
        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>👥 Relatos comunitários • confiança 30–98%</Text>
          <Text style={estilos.corpo}>Começam em 50% e sobem com relatos parecidos no bairro (+10% cada), confirmação por sensor (+20%) e texto coerente com a categoria (+10%). Texto incoerente reduz 15%. Validação em campo por agente garante no mínimo 90%.</Text>
        </View>

        <Text style={estilos.secao}>Ciclo de vida de um evento</Text>
        <View style={estilos.cartao}>
          {[
            ['REPORTADO', 'Dado recebido (sensor, base pública ou relato).'],
            ['EM_VALIDACAO', 'Confiança < 60%: aguarda outras fontes ou um agente.'],
            ['CONFIRMADO', 'Confiança ≥ 60% (validação cruzada ou agente).'],
            ['ALERTA_EMITIDO', 'Prioridade ≥ 70: usuários são notificados.'],
            ['EM_MONITORAMENTO', 'Situação estabilizando; volta a alerta se piorar.'],
            ['ENCERRADO', 'Normalizado ou encerrado por agente.'],
            ['DESCARTADO', 'Falso/duplicado ou sem confirmação em 2 h.'],
          ].map(([k, txt]) => (
            <View key={k} style={[estilos.linhaHorizontal, { marginVertical: 3 }]}>
              <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: CORES_STATUS[k], marginRight: 8 }} />
              <Text style={[estilos.corpo, { flex: 1 }]}><Text style={{ fontWeight: 'bold', color: CORES.texto }}>{STATUS[k]}</Text> — {txt}</Text>
            </View>
          ))}
        </View>

        <Text style={estilos.secao}>Modelos de IA</Text>
        {carregando && <ActivityIndicator color={CORES.destaque} />}
        {!carregando && !modelo && <Text style={estilos.meta}>Conecte-se ao servidor para ver os parâmetros e métricas atuais dos modelos.</Text>}
        {modelo && (
          <>
            <View style={estilos.cartao}>
              <Text style={estilos.cartaoTitulo}>1. NLP dos relatos</Text>
              <Text style={estilos.linha}>{modelo.nlp.algoritmo}</Text>
              <Text style={estilos.linha}>• {modelo.nlp.exemplosTreino} exemplos rotulados • vocabulário de {modelo.nlp.vocabulario} radicais</Text>
              <Text style={estilos.linha}>• Acurácia categoria (validação cruzada): {modelo.nlp.acuraciaCategoriaLeaveOneOut}%</Text>
              <Text style={estilos.linha}>• Acurácia urgência (validação cruzada): {modelo.nlp.acuraciaUrgenciaLeaveOneOut}%</Text>
            </View>
            <View style={estilos.cartao}>
              <Text style={estilos.cartaoTitulo}>2. Detecção de anomalias em sensores</Text>
              <Text style={estilos.linha}>{modelo.anomalias.algoritmo}</Text>
              <Text style={estilos.linha}>• Limiar z = {modelo.anomalias.limiarZ} • {modelo.anomalias.confirmacao}</Text>
            </View>
            <View style={estilos.cartao}>
              <Text style={estilos.cartaoTitulo}>3. Priorização</Text>
              <Text style={estilos.linha}>{modelo.priorizacao.algoritmo} • intercepto {modelo.priorizacao.intercepto}</Text>
              {Object.entries(modelo.priorizacao.pesos || {}).map(([k, v]) => (
                <Text key={k} style={estilos.linha}>• {NOMES_PESOS[k] || k}: peso {v}</Text>
              ))}
              <Text style={estilos.meta}>Faixas: {modelo.priorizacao.faixas} • meia-vida da recência {modelo.priorizacao.meiaVidaRecenciaMin} min</Text>
            </View>
          </>
        )}
      </ScrollView>
    </Tela>
  );
}
