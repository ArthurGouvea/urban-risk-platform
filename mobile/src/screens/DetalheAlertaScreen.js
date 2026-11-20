import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Linking, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { NivelBadge } from '../components/AlertaCard';
import { estilos } from '../components/estilos';
import { ACOES, CATEGORIAS, CORES, CORES_STATUS, NIVEIS, ORIGENS, STATUS, perfilPrivilegiado } from '../constants';
import { buscarHistorico } from '../services/api';
import { parseData, tempoRelativo } from '../utils/alertas';
import { useAgora } from '../components/useAgora';

function Barra({ valor, cor }) {
  return (
    <View style={s.barraFundo} accessibilityLabel={`${valor} de 100`}>
      <View style={[s.barra, { width: `${Math.max(0, Math.min(100, valor || 0))}%`, backgroundColor: cor }]} />
    </View>
  );
}

function LinhaDoTempo({ historico }) {
  return historico.map((h, i) => {
    const cor = CORES_STATUS[h.statusNovo] || CORES.textoFraco;
    const ultimo = i === historico.length - 1;
    return (
      <View key={h.id ?? i} style={s.passo} accessible accessibilityLabel={`${STATUS[h.statusNovo] || h.statusNovo}: ${h.motivo}, por ${h.responsavel}`}>
        <View style={s.trilho}>
          <View style={[s.marco, { backgroundColor: cor }]} />
          {!ultimo && <View style={s.linhaTrilho} />}
        </View>
        <View style={{ flex: 1, paddingBottom: ultimo ? 0 : 12 }}>
          <Text style={[estilos.linha, { fontWeight: 'bold', color: cor === CORES.textoFraco ? CORES.texto : cor }]}>{STATUS[h.statusNovo] || h.statusNovo}</Text>
          <Text style={estilos.corpo}>{h.motivo}</Text>
          <Text style={estilos.meta}>{parseData(h.dataHora)?.toLocaleTimeString() || ''} • {h.responsavel}</Text>
        </View>
      </View>
    );
  });
}

export default function DetalheAlertaScreen({ route }) {
  const { alertas, conexao, preferencias, executarAcaoAgente } = useRisk();
  const agora = useAgora(5000);
  const id = route.params?.id ?? route.params?.alerta?.id;
  const [versaoAcao, setVersaoAcao] = useState(null);
  const [historico, setHistorico] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [executando, setExecutando] = useState(null);
  const [mensagem, setMensagem] = useState(null);
  // usa a versão mais recente da lista (atualiza em tempo real); se o alerta saiu da lista
  // (encerrado/descartado), mostra a versão devolvida pela ação ou a última conhecida
  const atual = alertas.find((a) => a.id === id);
  const alerta = atual || versaoAcao || route.params?.alerta;
  const status = alerta?.status;
  const privilegiado = perfilPrivilegiado(preferencias.perfil);

  useEffect(() => {
    if (!id || conexao !== 'online') return;
    buscarHistorico(id).then(setHistorico).catch(() => {});
  }, [id, status, conexao]);

  const agir = async (acao) => {
    setExecutando(acao);
    setMensagem(null);
    try {
      const novo = await executarAcaoAgente(id, acao, motivo);
      setVersaoAcao(novo);
      setMotivo('');
      setMensagem({ ok: true, texto: `${ACOES[acao].label}: evento agora está "${STATUS[novo.status] || novo.status}".` });
      buscarHistorico(id).then(setHistorico).catch(() => {});
    } catch (e) {
      setMensagem({ ok: false, texto: e.message });
    } finally {
      setExecutando(null);
    }
  };

  if (!alerta) {
    return (
      <Tela comCabecalho>
        <Text style={estilos.vazio}>Este alerta foi encerrado ou não está mais disponível.</Text>
      </Tela>
    );
  }

  const nivel = NIVEIS[alerta.nivelRisco] || { cor: CORES.textoFraco, label: alerta.nivelRisco };
  const cat = CATEGORIAS[alerta.categoria] || { label: alerta.categoria, icone: '⚠️' };
  const origem = ORIGENS[alerta.origemDado] || { label: alerta.origemDado, icone: '•' };
  const fatores = alerta.fatoresIa || [];
  const justificativas = alerta.justificativasConfianca || [];
  const temCoordenadas = alerta.latitude != null && alerta.longitude != null;

  return (
    <Tela comCabecalho>
      <ScrollView contentContainerStyle={estilos.scroll}>
        {!atual && <Text style={[estilos.meta, { color: '#FCD34D', marginBottom: 8 }]}>Evento fora da lista ativa (encerrado/descartado) — exibindo a última versão conhecida.</Text>}
        <View style={estilos.entreLados}>
          <NivelBadge nivel={alerta.nivelRisco} score={alerta.scorePrioridade} />
          <View style={[s.status, { backgroundColor: CORES_STATUS[alerta.status] || CORES.borda }]}>
            <Text style={s.statusTexto}>{STATUS[alerta.status] || alerta.status}</Text>
          </View>
        </View>
        <Text style={[estilos.titulo, { marginTop: 10 }]} accessibilityRole="header">{cat.icone} {alerta.titulo}</Text>
        <Text style={estilos.corpo}>{alerta.descricao}</Text>
        <Text style={estilos.meta}>
          {cat.label} • 📍 {alerta.bairro} • registrado {tempoRelativo(alerta.dataHora, agora)}
          {alerta.atualizadoEm && alerta.atualizadoEm !== alerta.dataHora ? ` • atualizado ${tempoRelativo(alerta.atualizadoEm, agora)}` : ''}
        </Text>

        <View style={[estilos.cartao, { marginTop: 16, borderLeftWidth: 5, borderLeftColor: CORES.destaque }]}>
          <Text style={estilos.cartaoTitulo}>🔎 Origem e nível de confiança do dado</Text>
          <Text style={estilos.linha}>{origem.icone} Fonte: {origem.label}</Text>
          <Text style={estilos.linha}>Confiança: {alerta.percentualConfianca}% ({alerta.nivelConfianca})</Text>
          <Barra valor={alerta.percentualConfianca} cor={CORES.destaque} />
          {justificativas.map((j, i) => <Text key={i} style={estilos.linha}>• {j}</Text>)}
          <Text style={estilos.meta}>Identificador (pseudônimo LGPD): {alerta.autorAnonimizado}</Text>
        </View>

        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>🤖 Por que este alerta tem essa prioridade?</Text>
          <Text style={estilos.linha}>Prioridade: {alerta.scorePrioridade}/100 ({nivel.label})</Text>
          <Barra valor={alerta.scorePrioridade} cor={nivel.cor} />
          {fatores.map((f, i) => (
            <Text key={i} style={[estilos.linha, i === 0 && { fontWeight: 'bold' }]}>{i === 0 ? '' : '• '}{f}</Text>
          ))}
          {alerta.categoriaSugeridaIa && alerta.categoriaSugeridaIa !== alerta.categoria && (
            <Text style={[estilos.linha, { color: '#FCD34D' }]}>Categoria sugerida pela IA: {(CATEGORIAS[alerta.categoriaSugeridaIa] || {}).label || alerta.categoriaSugeridaIa}</Text>
          )}
          <Text style={estilos.meta}>Modelo: regressão logística explicável + NLP Naive Bayes + detecção de anomalias.</Text>
        </View>

        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>🕒 Linha do tempo do evento</Text>
          {historico === null && conexao === 'online' && <ActivityIndicator color={CORES.destaque} />}
          {historico === null && conexao !== 'online' && <Text style={estilos.meta}>Disponível quando o servidor estiver conectado.</Text>}
          {historico && <LinhaDoTempo historico={historico} />}
        </View>

        {privilegiado && (
          <View style={[estilos.cartao, { borderWidth: 1, borderColor: CORES.destaque }]}>
            <Text style={estilos.cartaoTitulo}>🦺 Ações do {preferencias.perfil}</Text>
            {(alerta.acoesDisponiveis || []).length === 0 ? (
              <Text style={estilos.meta}>Evento em estado final: nenhuma ação disponível.</Text>
            ) : (
              <>
                <TextInput
                  style={[estilos.input, { marginBottom: 6 }]}
                  value={motivo}
                  onChangeText={setMotivo}
                  placeholder="Motivo / observação (opcional)"
                  placeholderTextColor={CORES.textoFraco}
                  maxLength={300}
                  accessibilityLabel="Motivo da ação"
                />
                {(alerta.acoesDisponiveis || []).map((acao) => (
                  <TouchableOpacity
                    key={acao}
                    style={[estilos.botaoSecundario, acao === 'DESCARTAR' && { backgroundColor: '#3F1D1D' }]}
                    onPress={() => agir(acao)}
                    disabled={!!executando}
                    accessibilityRole="button"
                  >
                    {executando === acao
                      ? <ActivityIndicator color={CORES.destaque} />
                      : <Text style={[estilos.botaoSecundarioTexto, acao === 'DESCARTAR' && { color: '#FCA5A5' }]}>{(ACOES[acao] || {}).icone} {(ACOES[acao] || {}).label || acao}</Text>}
                  </TouchableOpacity>
                ))}
              </>
            )}
            {mensagem && <Text style={[estilos.meta, { color: mensagem.ok ? '#86EFAC' : '#FCA5A5' }]} accessibilityRole="alert">{mensagem.texto}</Text>}
          </View>
        )}

        {alerta.origemDado === 'SENSOR_IOT' && (
          <View style={estilos.cartao}>
            <Text style={estilos.cartaoTitulo}>📡 Leitura do sensor</Text>
            <Text style={estilos.linha}>Sensor: {alerta.sensorId}</Text>
            {alerta.valorMedido != null && <Text style={estilos.linha}>Valor: {alerta.valorMedido} {alerta.unidadeMedida}</Text>}
            {alerta.escoreZ != null && <Text style={estilos.linha}>Desvio da média (z-score): {Number(alerta.escoreZ).toFixed(1)}</Text>}
            {alerta.intensidadeAnomalia != null && <Text style={estilos.linha}>Intensidade da anomalia: {Math.round(alerta.intensidadeAnomalia * 100)}%</Text>}
          </View>
        )}

        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>🗺️ Localização</Text>
          <Text style={estilos.linha}>Bairro: {alerta.bairro}</Text>
          {temCoordenadas && <Text style={estilos.linha}>Coordenadas: {alerta.latitude}, {alerta.longitude}</Text>}
          {temCoordenadas && (
            <TouchableOpacity
              style={estilos.botaoSecundario}
              onPress={() => Linking.openURL(`https://www.google.com/maps/search/?api=1&query=${alerta.latitude},${alerta.longitude}`)}
              accessibilityRole="link"
            >
              <Text style={estilos.botaoSecundarioTexto}>Abrir no mapa</Text>
            </TouchableOpacity>
          )}
          <Text style={estilos.meta}>Registrado em {parseData(alerta.dataHora)?.toLocaleString() || '—'}</Text>
        </View>
      </ScrollView>
    </Tela>
  );
}

const s = StyleSheet.create({
  status: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  statusTexto: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 12 },
  passo: { flexDirection: 'row' },
  trilho: { width: 22, alignItems: 'center' },
  marco: { width: 12, height: 12, borderRadius: 6, marginTop: 6 },
  linhaTrilho: { flex: 1, width: 2, backgroundColor: CORES.borda, marginTop: 2 },
  barraFundo: { height: 10, backgroundColor: CORES.borda, borderRadius: 5, overflow: 'hidden', marginVertical: 6 },
  barra: { height: 10, borderRadius: 5 },
});
