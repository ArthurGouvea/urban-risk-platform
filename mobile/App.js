import React, { useState, useEffect, useCallback } from 'react';
import {
  StyleSheet, Text, View, FlatList, TouchableOpacity, TextInput,
  ScrollView, SafeAreaView, StatusBar, Alert, Switch, ActivityIndicator
} from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';

// TROQUE PELO SEU IPV4 (ex: http://192.168.0.15:8080/api)
const API_URL = 'http://localhost:8080/api';
const CACHE_KEY = '@urban_risk_alertas_cache';

const api = axios.create({
  baseURL: API_URL,
  timeout: 3000, // 3 segundos para detectar queda rapidamente na demo
});

// ================= CONTEXTO GLOBAL (DADOS + RESILIÊNCIA OFFLINE) =================
const RiskContext = React.createContext();

function RiskProvider({ children }) {
  const [alertas, setAlertas] = useState([]);
  const [isOnline, setIsOnline] = useState(true);
  const [ultimaAtualizacao, setUltimaAtualizacao] = useState('--:--:--');
  const [perfil, setPerfil] = useState('Morador'); // Morador ou Agente Local
  const [lgpdAceito, setLgpdAceito] = useState(false);
  const [loading, setLoading] = useState(true);

  const carregarAlertas = useCallback(async () => {
    try {
      const response = await api.get('/alertas');
      setAlertas(response.data);
      setIsOnline(true);
      setUltimaAtualizacao(new Date().toLocaleTimeString());
      await AsyncStorage.setItem(CACHE_KEY, JSON.stringify(response.data));
    } catch (error) {
      // Backend caiu: não trava o app, avisa e carrega cache local
      setIsOnline(false);
      const cached = await AsyncStorage.getItem(CACHE_KEY);
      if (cached) {
        setAlertas(JSON.parse(cached));
      }
    } finally {
      setLoading(false);
    }
  }, []);

  // Atualização em tempo real a cada 5 segundos + auto-reconexão
  useEffect(() => {
    carregarAlertas();
    const interval = setInterval(carregarAlertas, 5000);
    return () => clearInterval(interval);
  }, [carregarAlertas]);

  return (
    <RiskContext.Provider value={{
      alertas, isOnline, ultimaAtualizacao, perfil, setPerfil,
      lgpdAceito, setLgpdAceito, carregarAlertas, loading
    }}>
      {children}
    </RiskContext.Provider>
  );
}

// ================= COMPONENTE DE BANNER DE CONEXÃO =================
function ConnectionBanner() {
  const { isOnline, ultimaAtualizacao } = React.useContext(RiskContext);
  if (isOnline) {
    return (
      <View style={styles.bannerOnline}>
        <Text style={styles.bannerText}>● Conectado ao Servidor • Sincronizado às {ultimaAtualizacao}</Text>
      </View>
    );
  }
  return (
    <View style={styles.bannerOffline}>
      <Text style={styles.bannerText}>⚠️ SERVIDOR DESCONECTADO • Modo Offline (Cache Local)</Text>
    </View>
  );
}

// ================= TELA 1: BOAS-VINDAS & CONSENTIMENTO LGPD =================
function OnboardingScreen({ navigation }) {
  const { perfil, setPerfil, setLgpdAceito } = React.useContext(RiskContext);
  const [aceite, setAceite] = useState(true);

  const entrar = () => {
    if (!aceite) {
      Alert.alert('LGPD', 'É necessário aceitar os termos de anonimização de dados para continuar.');
      return;
    }
    setLgpdAceito(true);
    navigation.replace('MainTabs');
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.tag}>CITIZENTECH • FIAP 2026</Text>
        <Text style={styles.title}>Urban Risk Intelligence</Text>
        <Text style={styles.subtitle}>
          Monitoramento inteligente de alagamentos, ilhas de calor e serviços essenciais em tempo real.
        </Text>

        <Text style={styles.label}>1. Selecione seu perfil de acesso:</Text>
        <View style={styles.row}>
          {['Morador', 'Agente Local'].map((item) => (
            <TouchableOpacity
              key={item}
              style={[styles.chip, perfil === item && styles.chipActive]}
              onPress={() => setPerfil(item)}
            >
              <Text style={[styles.chipText, perfil === item && styles.chipTextActive]}>{item}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Privacidade e Proteção de Dados (LGPD)</Text>
          <Text style={styles.cardBody}>
            Seus relatos comunitários são processados com identificador anônimo (hash UUID). Nenhum dado pessoal sensível é exposto publicamente.
          </Text>
          <View style={styles.switchRow}>
            <Switch value={aceite} onValueChange={setAceite} trackColor={{ true: '#2563EB' }} />
            <Text style={styles.switchLabel}>Concordo com a política de dados</Text>
          </View>
        </View>

        <TouchableOpacity style={styles.primaryBtn} onPress={entrar}>
          <Text style={styles.primaryBtnText}>Acessar Plataforma</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= TELA 2: PAINEL DE ACOMPANHAMENTO (HOME) =================
function DashboardScreen({ navigation }) {
  const { alertas, perfil, loading } = React.useContext(RiskContext);

  const criticos = alertas.filter((a) => a.nivelRisco === 'CRITICO').length;
  const altos = alertas.filter((a) => a.nivelRisco === 'ALTO').length;
  const moderados = alertas.filter((a) => a.nivelRisco === 'MODERADO' || a.nivelRisco === 'BAIXO').length;

  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.header}>Painel Geral ({perfil})</Text>
        <Text style={styles.subtitle}>Resumo da situação urbana por severidade</Text>

        {loading ? <ActivityIndicator size="large" color="#2563EB" /> : (
          <>
            <View style={styles.statsRow}>
              <View style={[styles.statBox, { borderLeftColor: '#DC2626' }]}>
                <Text style={[styles.statNum, { color: '#DC2626' }]}>{criticos}</Text>
                <Text style={styles.statLabel}>Críticos</Text>
              </View>
              <View style={[styles.statBox, { borderLeftColor: '#EA580C' }]}>
                <Text style={[styles.statNum, { color: '#EA580C' }]}>{altos}</Text>
                <Text style={styles.statLabel}>Risco Alto</Text>
              </View>
              <View style={[styles.statBox, { borderLeftColor: '#CA8A04' }]}>
                <Text style={[styles.statNum, { color: '#CA8A04' }]}>{moderados}</Text>
                <Text style={styles.statLabel}>Moderados</Text>
              </View>
            </View>

            <Text style={styles.sectionTitle}>Mapa / Zonas Monitoradas</Text>
            {alertas.slice(0, 3).map((item) => (
              <TouchableOpacity
                key={item.id}
                style={styles.card}
                onPress={() => navigation.navigate('DetalheAlerta', { alerta: item })}
              >
                <View style={styles.rowBetween}>
                  <Text style={styles.badgeRisk}>{item.nivelRisco} • Score {item.scorePrioridade}</Text>
                  <Text style={styles.metaText}>{item.bairro}</Text>
                </View>
                <Text style={styles.cardTitle}>{item.titulo}</Text>
                <Text style={styles.metaText}>
                  Fonte: {item.origemDado} | Confiança: {item.nivelConfianca} ({item.percentualConfianca}%)
                </Text>
              </TouchableOpacity>
            ))}

            <TouchableOpacity
              style={styles.secondaryBtn}
              onPress={() => navigation.navigate('Transparencia')}
            >
              <Text style={styles.secondaryBtnText}>Ver Fontes de Dados & Motor de Confiança</Text>
            </TouchableOpacity>
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= TELA 3: LISTA DE ALERTAS PRIORIZADOS =================
function ListaAlertasScreen({ navigation }) {
  const { alertas } = React.useContext(RiskContext);
  const [filtro, setFiltro] = useState('TODOS');

  const filtrados = filtro === 'TODOS'
    ? alertas
    : alertas.filter((a) => a.categoria === filtro);

  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <View style={styles.scroll}>
        <Text style={styles.header}>Alertas Priorizados</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 12, maxHeight: 40 }}>
          {['TODOS', 'ALAGAMENTO', 'FALTA_ENERGIA', 'ILHA_DE_CALOR', 'FALTA_AGUA'].map((cat) => (
            <TouchableOpacity
              key={cat}
              style={[styles.chip, filtro === cat && styles.chipActive]}
              onPress={() => setFiltro(cat)}
            >
              <Text style={[styles.chipText, filtro === cat && styles.chipTextActive]}>{cat}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <FlatList
          data={filtrados}
          keyExtractor={(item) => String(item.id)}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.card}
              onPress={() => navigation.navigate('DetalheAlerta', { alerta: item })}
            >
              <View style={styles.rowBetween}>
                <Text style={styles.badgeRisk}>{item.nivelRisco} ({item.scorePrioridade} pts)</Text>
                <Text style={styles.metaText}>{item.status}</Text>
              </View>
              <Text style={styles.cardTitle}>{item.titulo}</Text>
              <Text style={styles.cardBody} numberOfLines={2}>{item.descricao}</Text>
              <Text style={styles.metaText}>
                📍 {item.bairro} • Origem: {item.origemDado} ({item.percentualConfianca}%)
              </Text>
            </TouchableOpacity>
          )}
        />
      </View>
    </SafeAreaView>
  );
}

// ================= TELA 4: DETALHE DO ALERTA (ORIGEM E CONFIANÇA) =================
function DetalheAlertaScreen({ route }) {
  const { alerta } = route.params;
  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.tag}>{alerta.categoria} • {alerta.status}</Text>
        <Text style={styles.title}>{alerta.titulo}</Text>
        <Text style={styles.cardBody}>{alerta.descricao}</Text>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Auditoria de Origem e Confiança</Text>
          <Text style={styles.detailLine}>• Origem do Dado: {alerta.origemDado}</Text>
          <Text style={styles.detailLine}>• Nível de Confiança: {alerta.nivelConfianca} ({alerta.percentualConfianca}%)</Text>
          <Text style={styles.detailLine}>• Score de Prioridade IA: {alerta.scorePrioridade} / 100</Text>
          <Text style={styles.detailLine}>• Classificação de Risco: {alerta.nivelRisco}</Text>
          <Text style={styles.detailLine}>• Identificador (LGPD): {alerta.autorAnonimizado}</Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Localização Geográfica</Text>
          <Text style={styles.detailLine}>• Bairro: {alerta.bairro}</Text>
          <Text style={styles.detailLine}>• Coordenadas: {alerta.latitude || -23.55}, {alerta.longitude || -46.63}</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= TELA 5: REGISTRO COMUNITÁRIO DE OCORRÊNCIA =================
function ReportarScreen({ navigation }) {
  const { carregarAlertas, isOnline } = React.useContext(RiskContext);
  const [titulo, setTitulo] = useState('');
  const [bairro, setBairro] = useState('');
  const [descricao, setDescricao] = useState('');
  const [categoria, setCategoria] = useState('ALAGAMENTO');
  const [enviando, setEnviando] = useState(false);

  const enviarRelato = async () => {
    if (!titulo || !bairro || !descricao) {
      Alert.alert('Campos obrigatórios', 'Preencha título, bairro e descrição.');
      return;
    }
    if (!isOnline) {
      Alert.alert('Modo Offline', 'Servidor desconectado. Reconecte para enviar novos relatos.');
      return;
    }
    setEnviando(true);
    try {
      await api.post('/alertas', {
        titulo, bairro, descricao, categoria,
        origemDado: 'REGISTRO_COMUNITARIO',
        latitude: -23.56, longitude: -46.64
      });
      await carregarAlertas();
      setTitulo(''); setBairro(''); setDescricao('');
      Alert.alert('Sucesso', 'Ocorrência enviada e priorizada pelo motor de risco!');
      navigation.navigate('Alertas');
    } catch (e) {
      Alert.alert('Erro', 'Não foi possível enviar o relato agora.');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.header}>Reportar Ocorrência</Text>
        <Text style={styles.subtitle}>Seu relato será cruzado com sensores e anonimizado (LGPD)</Text>

        <Text style={styles.label}>Categoria do Risco</Text>
        <View style={styles.rowWrap}>
          {['ALAGAMENTO', 'FALTA_ENERGIA', 'ILHA_DE_CALOR', 'FALTA_AGUA'].map((cat) => (
            <TouchableOpacity
              key={cat}
              style={[styles.chip, categoria === cat && styles.chipActive]}
              onPress={() => setCategoria(cat)}
            >
              <Text style={[styles.chipText, categoria === cat && styles.chipTextActive]}>{cat}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.label}>Título da Ocorrência</Text>
        <TextInput style={styles.input} placeholder="Ex: Rua alagada próximo à praça" placeholderTextColor="#94A3B8" value={titulo} onChangeText={setTitulo} />

        <Text style={styles.label}>Bairro</Text>
        <TextInput style={styles.input} placeholder="Ex: Pinheiros" placeholderTextColor="#94A3B8" value={bairro} onChangeText={setBairro} />

        <Text style={styles.label}>Descrição Detalhada</Text>
        <TextInput style={[styles.input, { height: 90 }]} multiline placeholder="Descreva a situação atual..." placeholderTextColor="#94A3B8" value={descricao} onChangeText={setDescricao} />

        <TouchableOpacity style={styles.primaryBtn} onPress={enviarRelato} disabled={enviando}>
          <Text style={styles.primaryBtnText}>{enviando ? 'Processando...' : 'Enviar Relato Comunitário'}</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= TELA 6: CENTRAL DE NOTIFICAÇÕES =================
function NotificacoesScreen({ navigation }) {
  const { alertas } = React.useContext(RiskContext);
  const urgentes = alertas.filter((a) => a.scorePrioridade >= 70);

  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.header}>Central de Notificações</Text>
        <Text style={styles.subtitle}>Alertas ativos com prioridade alta ou crítica</Text>

        {urgentes.map((item) => (
          <TouchableOpacity
            key={item.id}
            style={[styles.card, { borderLeftWidth: 4, borderLeftColor: '#DC2626' }]}
            onPress={() => navigation.navigate('DetalheAlerta', { alerta: item })}
          >
            <Text style={styles.badgeRisk}>🔔 ALERTA {item.nivelRisco} • {item.bairro}</Text>
            <Text style={styles.cardTitle}>{item.titulo}</Text>
            <Text style={styles.metaText}>Confiança: {item.nivelConfianca} ({item.percentualConfianca}%)</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= TELA 7: TRANSPARÊNCIA & FONTES DE DADOS =================
function TransparenciaScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.header}>Fontes & Confiabilidade</Text>
        <Text style={styles.subtitle}>Como a plataforma valida e prioriza cada evento</Text>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>1. Sensores IoT Urbanos (Confiança: 90% - 98%)</Text>
          <Text style={styles.cardBody}>Estações pluviométricas e térmicas automáticas com telemetria contínua.</Text>
        </View>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>2. Bases Públicas Oficiais (Confiança: 92% - 96%)</Text>
          <Text style={styles.cardBody}>Integração com boletins da Defesa Civil, concessionárias de energia e saneamento.</Text>
        </View>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>3. Registros Comunitários (Confiança: 60% - 88%)</Text>
          <Text style={styles.cardBody}>Relatos de moradores iniciam em validação (62%) e sobem para Alta Confiança (88%) quando cruzados com outros relatos no mesmo bairro.</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= TELA 8: PERFIL & CONFIGURAÇÕES LGPD =================
function PerfilScreen({ navigation }) {
  const { perfil, setPerfil, isOnline, setLgpdAceito } = React.useContext(RiskContext);

  return (
    <SafeAreaView style={styles.container}>
      <ConnectionBanner />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.header}>Perfil e Configurações</Text>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Modo de Operação Atual: {perfil}</Text>
          <TouchableOpacity
            style={styles.secondaryBtn}
            onPress={() => setPerfil(perfil === 'Morador' ? 'Agente Local' : 'Morador')}
          >
            <Text style={styles.secondaryBtnText}>Alternar para {perfil === 'Morador' ? 'Agente Local' : 'Morador'}</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>Status da API Spring Boot</Text>
          <Text style={styles.detailLine}>• Endpoint: {API_URL}</Text>
          <Text style={styles.detailLine}>• Estado: {isOnline ? 'ONLINE (Tempo Real)' : 'OFFLINE (Cache Ativo)'}</Text>
        </View>

        <TouchableOpacity
          style={[styles.primaryBtn, { backgroundColor: '#DC2626' }]}
          onPress={() => { setLgpdAceito(false); navigation.replace('Onboarding'); }}
        >
          <Text style={styles.primaryBtnText}>Revogar Consentimento LGPD / Sair</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

// ================= NAVEGAÇÃO PRINCIPAL =================
const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

function MainTabs() {
  return (
    <Tab.Navigator screenOptions={{ headerShown: false, tabBarStyle: { backgroundColor: '#0F172A', borderTopColor: '#1E293B' }, tabBarActiveTintColor: '#38BDF8', tabBarInactiveTintColor: '#94A3B8' }}>
      <Tab.Screen name="Painel" component={DashboardScreen} />
      <Tab.Screen name="Alertas" component={ListaAlertasScreen} />
      <Tab.Screen name="Reportar" component={ReportarScreen} />
      <Tab.Screen name="Avisos" component={NotificacoesScreen} />
      <Tab.Screen name="Perfil" component={PerfilScreen} />
    </Tab.Navigator>
  );
}

export default function App() {
  return (
    <RiskProvider>
      <StatusBar barStyle="light-content" backgroundColor="#0F172A" />
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerStyle: { backgroundColor: '#0F172A' }, headerTintColor: '#F8FAFC' }}>
          <Stack.Screen name="Onboarding" component={OnboardingScreen} options={{ headerShown: false }} />
          <Stack.Screen name="MainTabs" component={MainTabs} options={{ headerShown: false }} />
          <Stack.Screen name="DetalheAlerta" component={DetalheAlertaScreen} options={{ title: 'Detalhes do Evento' }} />
          <Stack.Screen name="Transparencia" component={TransparenciaScreen} options={{ title: 'Fontes de Dados' }} />
        </Stack.Navigator>
      </NavigationContainer>
    </RiskProvider>
  );
}

// ================= ESTILOS =================
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0F172A' },
  scroll: { padding: 16, paddingBottom: 32 },
  bannerOnline: { backgroundColor: '#065F46', padding: 8, alignItems: 'center' },
  bannerOffline: { backgroundColor: '#B91C1C', padding: 10, alignItems: 'center' },
  bannerText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 12 },
  tag: { color: '#38BDF8', fontWeight: 'bold', fontSize: 12, marginBottom: 4 },
  title: { color: '#F8FAFC', fontSize: 26, fontWeight: 'bold', marginBottom: 8 },
  header: { color: '#F8FAFC', fontSize: 22, fontWeight: 'bold', marginBottom: 4 },
  subtitle: { color: '#94A3B8', fontSize: 14, marginBottom: 16 },
  sectionTitle: { color: '#F8FAFC', fontSize: 16, fontWeight: 'bold', marginTop: 12, marginBottom: 8 },
  label: { color: '#E2E8F0', fontSize: 14, fontWeight: '600', marginTop: 12, marginBottom: 6 },
  statsRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 12 },
  statBox: { flex: 1, backgroundColor: '#1E293B', padding: 12, borderRadius: 10, marginHorizontal: 4, borderLeftWidth: 4 },
  statNum: { fontSize: 22, fontWeight: 'bold' },
  statLabel: { color: '#CBD5E1', fontSize: 12, marginTop: 2 },
  card: { backgroundColor: '#1E293B', padding: 14, borderRadius: 12, marginBottom: 12 },
  cardTitle: { color: '#F8FAFC', fontSize: 16, fontWeight: 'bold', marginVertical: 4 },
  cardBody: { color: '#CBD5E1', fontSize: 13, lineHeight: 19 },
  metaText: { color: '#94A3B8', fontSize: 12, marginTop: 4 },
  detailLine: { color: '#E2E8F0', fontSize: 14, marginVertical: 3 },
  badgeRisk: { color: '#38BDF8', fontWeight: 'bold', fontSize: 12 },
  row: { flexDirection: 'row', gap: 8, marginBottom: 12 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 8 },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  chip: { backgroundColor: '#1E293B', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 20, marginRight: 6, marginBottom: 6 },
  chipActive: { backgroundColor: '#2563EB' },
  chipText: { color: '#94A3B8', fontSize: 12, fontWeight: '600' },
  chipTextActive: { color: '#FFFFFF' },
  switchRow: { flexDirection: 'row', alignItems: 'center', marginTop: 12 },
  switchLabel: { color: '#E2E8F0', marginLeft: 10, fontSize: 13 },
  input: { backgroundColor: '#1E293B', color: '#F8FAFC', borderRadius: 10, padding: 12, fontSize: 14, borderWidth: 1, borderColor: '#334155' },
  primaryBtn: { backgroundColor: '#2563EB', padding: 15, borderRadius: 12, alignItems: 'center', marginTop: 18 },
  primaryBtnText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 15 },
  secondaryBtn: { backgroundColor: '#334155', padding: 12, borderRadius: 10, alignItems: 'center', marginTop: 8 },
  secondaryBtnText: { color: '#38BDF8', fontWeight: 'bold', fontSize: 13 }
});