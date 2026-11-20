import React, { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, Switch, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { estilos } from '../components/estilos';
import { CATEGORIAS, CORES, NIVEIS, STATUS, perfilPrivilegiado } from '../constants';
import { analisarTexto } from '../services/api';

export default function ReportarScreen({ navigation }) {
  const { registrarRelato, conexao, preferencias } = useRisk();
  const podePublicarOficial = perfilPrivilegiado(preferencias.perfil);
  const [oficial, setOficial] = useState(false);
  const [categoria, setCategoria] = useState('ALAGAMENTO');
  const [titulo, setTitulo] = useState('');
  const [bairro, setBairro] = useState('');
  const [descricao, setDescricao] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [sugestao, setSugestao] = useState(null);
  const [resultado, setResultado] = useState(null);

  // IA em tempo real: enquanto o morador digita, o backend sugere a categoria e estima a urgência
  useEffect(() => {
    const texto = `${titulo}. ${descricao}`.trim();
    if (conexao !== 'online' || texto.length < 12) {
      setSugestao(null);
      return undefined;
    }
    const t = setTimeout(() => {
      analisarTexto(texto, categoria).then(setSugestao).catch(() => setSugestao(null));
    }, 700);
    return () => clearTimeout(t);
  }, [titulo, descricao, categoria, conexao]);

  const enviar = async () => {
    setResultado(null);
    if (!titulo.trim() || !bairro.trim() || !descricao.trim()) {
      setResultado({ tipo: 'erro', texto: 'Preencha título, bairro e descrição.' });
      return;
    }
    setEnviando(true);
    try {
      const r = await registrarRelato({
        titulo: titulo.trim(), bairro: bairro.trim(), descricao: descricao.trim(), categoria,
        origemDado: podePublicarOficial && oficial ? 'BASE_PUBLICA' : 'REGISTRO_COMUNITARIO',
      });
      setTitulo(''); setBairro(''); setDescricao(''); setSugestao(null);
      if (r.status === 'enviado') {
        const n = NIVEIS[r.alerta.nivelRisco] || {};
        setResultado({
          tipo: 'ok',
          texto: `${r.alerta.origemDado === 'BASE_PUBLICA' ? 'Boletim oficial publicado' : 'Relato enviado'}! A IA calculou prioridade ${r.alerta.scorePrioridade}/100 (${n.label || r.alerta.nivelRisco}) e confiança ${r.alerta.percentualConfianca}%. Situação: ${STATUS[r.alerta.status] || r.alerta.status}.`,
          alerta: r.alerta,
        });
      } else {
        setResultado({ tipo: 'fila', texto: 'Sem conexão: o relato foi guardado no aparelho e será enviado automaticamente quando o servidor voltar.' });
      }
    } catch (e) {
      setResultado({ tipo: 'erro', texto: e.message });
    } finally {
      setEnviando(false);
    }
  };

  const divergente = sugestao && sugestao.palavrasReconhecidas > 0 && sugestao.categoriaSugerida !== categoria;
  const corResultado = { ok: CORES.sucesso, fila: CORES.reconectando, erro: CORES.offline };

  return (
    <Tela>
      <ScrollView contentContainerStyle={estilos.scroll} keyboardShouldPersistTaps="handled">
        <Text style={estilos.cabecalho} accessibilityRole="header">Reportar ocorrência</Text>
        <Text style={estilos.subtitulo}>Seu relato é cruzado com sensores e outros relatos. Dados pessoais são removidos (LGPD).</Text>

        {podePublicarOficial && (
          <View style={[estilos.cartao, estilos.entreLados]}>
            <View style={{ flex: 1, paddingRight: 8 }}>
              <Text style={estilos.cartaoTitulo}>🏛️ Publicar como boletim oficial</Text>
              <Text style={estilos.meta}>Origem "Base pública" (confiança 95%). Exige código de acesso válido.</Text>
            </View>
            <Switch value={oficial} onValueChange={setOficial} trackColor={{ true: CORES.primaria }} accessibilityLabel="Publicar como boletim oficial" />
          </View>
        )}

        <Text style={estilos.rotulo}>Tipo de risco</Text>
        <View style={estilos.linhaQuebra}>
          {Object.entries(CATEGORIAS).map(([k, v]) => (
            <TouchableOpacity
              key={k}
              style={[estilos.chip, categoria === k && estilos.chipAtivo]}
              onPress={() => setCategoria(k)}
              accessibilityRole="radio"
              accessibilityState={{ selected: categoria === k }}
            >
              <Text style={[estilos.chipTexto, categoria === k && estilos.chipTextoAtivo]}>{v.icone} {v.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={estilos.rotulo}>O que está acontecendo?</Text>
        <TextInput style={estilos.input} placeholder="Ex.: Rua alagada perto da praça" placeholderTextColor={CORES.textoFraco}
          value={titulo} onChangeText={setTitulo} maxLength={120} accessibilityLabel="Título da ocorrência" />

        <Text style={estilos.rotulo}>Bairro</Text>
        <TextInput style={estilos.input} placeholder="Ex.: Pinheiros" placeholderTextColor={CORES.textoFraco}
          value={bairro} onChangeText={setBairro} maxLength={80} accessibilityLabel="Bairro" />

        <Text style={estilos.rotulo}>Detalhes</Text>
        <TextInput style={[estilos.input, { height: 100, textAlignVertical: 'top' }]} multiline
          placeholder="Descreva a situação (não informe telefone, CPF ou e-mail)" placeholderTextColor={CORES.textoFraco}
          value={descricao} onChangeText={setDescricao} maxLength={1000} accessibilityLabel="Descrição detalhada" />

        {sugestao && (
          <View style={[estilos.cartao, { marginTop: 12, borderLeftWidth: 5, borderLeftColor: divergente ? '#FCD34D' : CORES.destaque }]} accessibilityLiveRegion="polite">
            <Text style={estilos.cartaoTitulo}>🤖 Análise da IA</Text>
            <Text style={estilos.linha}>
              Categoria provável: {(CATEGORIAS[sugestao.categoriaSugerida] || {}).label} ({Math.round(sugestao.probabilidadeCategoriaSugerida * 100)}%)
            </Text>
            <Text style={estilos.linha}>Urgência estimada: {sugestao.probabilidadeUrgencia >= 0.5 ? 'ALTA' : 'baixa'} ({Math.round(sugestao.probabilidadeUrgencia * 100)}%)</Text>
            {divergente && (
              <TouchableOpacity style={estilos.botaoSecundario} onPress={() => setCategoria(sugestao.categoriaSugerida)} accessibilityRole="button">
                <Text style={estilos.botaoSecundarioTexto}>Usar sugestão: {(CATEGORIAS[sugestao.categoriaSugerida] || {}).label}</Text>
              </TouchableOpacity>
            )}
          </View>
        )}

        {conexao !== 'online' && (
          <Text style={[estilos.meta, { color: '#FCD34D' }]}>Você está offline: o relato será guardado e enviado ao reconectar.</Text>
        )}

        <TouchableOpacity style={[estilos.botaoPrimario, enviando && { opacity: 0.6 }]} onPress={enviar} disabled={enviando} accessibilityRole="button">
          {enviando ? <ActivityIndicator color="#FFF" /> : <Text style={estilos.botaoPrimarioTexto}>{conexao !== 'online' ? 'Guardar e enviar depois' : oficial && podePublicarOficial ? 'Publicar boletim oficial' : 'Enviar relato'}</Text>}
        </TouchableOpacity>

        {resultado && (
          <View style={[estilos.cartao, { marginTop: 16, borderLeftWidth: 5, borderLeftColor: corResultado[resultado.tipo] }]} accessibilityRole="alert">
            <Text style={estilos.corpo}>{resultado.texto}</Text>
            {resultado.alerta && (
              <TouchableOpacity style={estilos.botaoSecundario} onPress={() => navigation.navigate('DetalheAlerta', { id: resultado.alerta.id, alerta: resultado.alerta })}>
                <Text style={estilos.botaoSecundarioTexto}>Ver análise completa</Text>
              </TouchableOpacity>
            )}
          </View>
        )}
      </ScrollView>
    </Tela>
  );
}
