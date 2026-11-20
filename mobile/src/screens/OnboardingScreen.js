import React, { useState } from 'react';
import { ScrollView, Switch, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRisk } from '../context/RiskContext';
import { estilos } from '../components/estilos';
import { CORES, PERFIS, perfilPrivilegiado } from '../constants';

export default function OnboardingScreen({ navigation }) {
  const { preferencias, atualizarPreferencias } = useRisk();
  const [perfil, setPerfil] = useState(preferencias.perfil);
  const [token, setToken] = useState(preferencias.tokenAgente || '');
  const [aceite, setAceite] = useState(false);
  const [erro, setErro] = useState('');

  const entrar = () => {
    if (!aceite) {
      setErro('Para continuar, aceite a política de proteção de dados.');
      return;
    }
    if (perfilPrivilegiado(perfil) && !token.trim()) {
      setErro('Informe o código de acesso fornecido pela Defesa Civil / concessionária.');
      return;
    }
    atualizarPreferencias({ perfil, tokenAgente: perfilPrivilegiado(perfil) ? token.trim() : '', lgpdAceito: true });
    navigation.replace('MainTabs');
  };

  return (
    <SafeAreaView style={estilos.container}>
      <ScrollView contentContainerStyle={estilos.scroll}>
        <Text style={estilos.tag}>CITIZENTECH • FIAP 2026</Text>
        <Text style={estilos.titulo} accessibilityRole="header">Urban Risk Intelligence</Text>
        <Text style={estilos.subtitulo}>
          Alertas de alagamento, ilhas de calor e falhas em serviços essenciais, priorizados por inteligência artificial e atualizados em tempo real.
        </Text>

        <Text style={estilos.rotulo}>1. Como você vai usar o app?</Text>
        {PERFIS.map((p) => (
          <TouchableOpacity
            key={p.id}
            style={[estilos.cartao, { borderWidth: 2, borderColor: perfil === p.id ? CORES.primaria : 'transparent' }]}
            onPress={() => setPerfil(p.id)}
            accessibilityRole="radio"
            accessibilityState={{ selected: perfil === p.id }}
          >
            <Text style={estilos.cartaoTitulo}>{perfil === p.id ? '◉' : '○'} {p.icone} {p.id}</Text>
            <Text style={estilos.corpo}>{p.descricao}</Text>
          </TouchableOpacity>
        ))}
        {perfilPrivilegiado(perfil) && (
          <View style={estilos.cartao}>
            <Text style={estilos.cartaoTitulo}>🔑 Código de acesso</Text>
            <Text style={estilos.meta}>Perfis institucionais podem validar, emitir e encerrar alertas. Use o código fornecido pelo órgão.</Text>
            <TextInput
              style={[estilos.input, { marginTop: 8 }]}
              value={token}
              onChangeText={(v) => { setToken(v); setErro(''); }}
              autoCapitalize="none"
              secureTextEntry
              placeholder="Código de acesso"
              placeholderTextColor={CORES.textoFraco}
              accessibilityLabel="Código de acesso do agente ou operador"
            />
          </View>
        )}

        <Text style={estilos.rotulo}>2. Privacidade (LGPD)</Text>
        <View style={estilos.cartao}>
          <Text style={estilos.corpo}>
            • Não pedimos nome, CPF, telefone ou e-mail.{'\n'}
            • Seus relatos usam um pseudônimo (hash) — ninguém vê quem enviou.{'\n'}
            • Telefones, e-mails, CPFs e placas digitados no texto são removidos automaticamente.{'\n'}
            • A localização dos relatos é aproximada (~100 m).{'\n'}
            • Você pode revogar o consentimento e apagar os dados locais a qualquer momento na aba Perfil.
          </Text>
          <View style={[estilos.linhaHorizontal, { marginTop: 12 }]}>
            <Switch
              value={aceite}
              onValueChange={(v) => { setAceite(v); setErro(''); }}
              trackColor={{ true: CORES.primaria }}
              accessibilityLabel="Concordo com a política de proteção de dados"
            />
            <Text style={[estilos.corpo, { marginLeft: 10, flex: 1 }]}>Li e concordo com a política de dados</Text>
          </View>
        </View>

        {!!erro && <Text style={{ color: '#FCA5A5', fontSize: 14 }} accessibilityRole="alert">{erro}</Text>}

        <TouchableOpacity style={estilos.botaoPrimario} onPress={entrar} accessibilityRole="button">
          <Text style={estilos.botaoPrimarioTexto}>Acessar plataforma</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}
