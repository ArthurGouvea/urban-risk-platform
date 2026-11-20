import React, { useState } from 'react';
import { ScrollView, Switch, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { estilos } from '../components/estilos';
import { CORES, PERFIS, perfilPrivilegiado } from '../constants';
import { verificarSaude } from '../services/api';
import { API_URL_PADRAO } from '../config';
import { navegacaoRef } from '../navegacao';

export default function PerfilScreen() {
  const { preferencias, atualizarPreferencias, conexao, latenciaMs, ultimaSincronizacao, filaRelatos, revogarConsentimento } = useRisk();
  const [url, setUrl] = useState(preferencias.apiUrl);
  const [teste, setTeste] = useState(null);
  const [confirmarRevogacao, setConfirmarRevogacao] = useState(false);
  const [token, setToken] = useState(preferencias.tokenAgente || '');
  const [tokenSalvo, setTokenSalvo] = useState(false);

  const testarESalvar = async () => {
    setTeste({ texto: 'Testando...', ok: null });
    try {
      const r = await verificarSaude(url);
      atualizarPreferencias({ apiUrl: url.trim() });
      setTeste({ texto: `Conectado (${r.latenciaMs} ms). Endereço salvo.`, ok: true });
    } catch (e) {
      setTeste({ texto: 'Não foi possível conectar nesse endereço. Verifique o IP e se o backend está rodando.', ok: false });
    }
  };

  const revogar = async () => {
    if (!confirmarRevogacao) { setConfirmarRevogacao(true); return; }
    await revogarConsentimento();
    navegacaoRef.reset({ index: 0, routes: [{ name: 'Onboarding' }] });
  };

  return (
    <Tela>
      <ScrollView contentContainerStyle={estilos.scroll} keyboardShouldPersistTaps="handled">
        <Text style={estilos.cabecalho} accessibilityRole="header">Perfil e configurações</Text>

        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>Perfil de uso</Text>
          {PERFIS.map((p) => {
            const ativo = preferencias.perfil === p.id;
            return (
              <TouchableOpacity
                key={p.id}
                style={[estilos.chip, ativo && estilos.chipAtivo, { alignSelf: 'flex-start' }]}
                onPress={() => atualizarPreferencias({ perfil: p.id })}
                accessibilityRole="radio"
                accessibilityState={{ selected: ativo }}
              >
                <Text style={[estilos.chipTexto, ativo && estilos.chipTextoAtivo]}>{p.icone} {p.id}</Text>
              </TouchableOpacity>
            );
          })}
          {perfilPrivilegiado(preferencias.perfil) && (
            <>
              <Text style={estilos.rotulo}>Código de acesso (agente/operador)</Text>
              <TextInput
                style={estilos.input}
                value={token}
                onChangeText={(v) => { setToken(v); setTokenSalvo(false); }}
                autoCapitalize="none"
                secureTextEntry
                placeholder="Código de acesso"
                placeholderTextColor={CORES.textoFraco}
                accessibilityLabel="Código de acesso do agente ou operador"
              />
              <TouchableOpacity style={estilos.botaoSecundario} onPress={() => { atualizarPreferencias({ tokenAgente: token.trim() }); setTokenSalvo(true); }} accessibilityRole="button">
                <Text style={estilos.botaoSecundarioTexto}>{tokenSalvo ? 'Código salvo ✓' : 'Salvar código'}</Text>
              </TouchableOpacity>
            </>
          )}
        </View>

        <View style={[estilos.cartao, estilos.entreLados]}>
          <Text style={[estilos.corpo, { flex: 1 }]}>Notificações no celular (alertas altos e críticos)</Text>
          <Switch
            value={preferencias.notificacoes}
            onValueChange={(v) => atualizarPreferencias({ notificacoes: v })}
            trackColor={{ true: CORES.primaria }}
            accessibilityLabel="Ativar notificações"
          />
        </View>

        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>Servidor (API Spring Boot)</Text>
          <Text style={estilos.linha}>Estado: {conexao === 'online' ? `🟢 Online${latenciaMs != null ? ` • ${latenciaMs} ms` : ''}` : conexao === 'offline' ? '🔴 Offline (usando cache local)' : '🟡 Conectando'}</Text>
          <Text style={estilos.linha}>Última sincronização: {ultimaSincronizacao ? ultimaSincronizacao.toLocaleTimeString() : '—'}</Text>
          <Text style={estilos.linha}>Relatos aguardando envio: {filaRelatos.length}</Text>
          <Text style={estilos.rotulo}>Endereço da API</Text>
          <TextInput
            style={estilos.input}
            value={url}
            onChangeText={setUrl}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="url"
            placeholder="http://192.168.0.10:8080/api"
            placeholderTextColor={CORES.textoFraco}
            accessibilityLabel="Endereço da API"
          />
          <Text style={estilos.meta}>Celular físico: use o IP do computador na mesma rede Wi-Fi. Padrão detectado: {API_URL_PADRAO}</Text>
          <TouchableOpacity style={estilos.botaoSecundario} onPress={testarESalvar} accessibilityRole="button">
            <Text style={estilos.botaoSecundarioTexto}>Testar e salvar</Text>
          </TouchableOpacity>
          {teste && <Text style={[estilos.meta, { color: teste.ok === false ? '#FCA5A5' : teste.ok ? '#86EFAC' : CORES.textoFraco }]}>{teste.texto}</Text>}
        </View>

        <View style={estilos.cartao}>
          <Text style={estilos.cartaoTitulo}>Privacidade (LGPD)</Text>
          <Text style={estilos.corpo}>Revogar o consentimento apaga do aparelho o cache, a fila de relatos e o identificador anônimo.</Text>
          <TouchableOpacity style={[estilos.botaoPrimario, { backgroundColor: '#DC2626' }]} onPress={revogar} accessibilityRole="button">
            <Text style={estilos.botaoPrimarioTexto}>{confirmarRevogacao ? 'Toque de novo para confirmar' : 'Revogar consentimento e sair'}</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </Tela>
  );
}
