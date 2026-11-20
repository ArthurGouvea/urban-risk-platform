import React from 'react';
import { ActivityIndicator, LogBox, Text, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { DarkTheme, NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { RiskProvider, useRisk } from './src/context/RiskContext';
import { navegacaoRef } from './src/navegacao';
import { AlertaToast } from './src/components/AlertaToast';
import { CORES } from './src/constants';
import OnboardingScreen from './src/screens/OnboardingScreen';
import DashboardScreen from './src/screens/DashboardScreen';
import ListaAlertasScreen from './src/screens/ListaAlertasScreen';
import DetalheAlertaScreen from './src/screens/DetalheAlertaScreen';
import ReportarScreen from './src/screens/ReportarScreen';
import NotificacoesScreen from './src/screens/NotificacoesScreen';
import TransparenciaScreen from './src/screens/TransparenciaScreen';
import PerfilScreen from './src/screens/PerfilScreen';
import MapaScreen from './src/screens/MapaScreen';

// Aviso do Expo Go sobre push REMOTO (não usado; notificações locais funcionam).
LogBox.ignoreLogs(['expo-notifications']);

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

const tema = {
  ...DarkTheme,
  colors: { ...DarkTheme.colors, background: CORES.fundo, card: CORES.fundo, primary: CORES.destaque, text: CORES.texto, border: CORES.cartao },
};

const icone = (emoji) => function Icone({ focused }) {
  return <Text style={{ fontSize: 20, opacity: focused ? 1 : 0.6 }}>{emoji}</Text>;
};

function MainTabs() {
  const { naoLidos } = useRisk();
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: { backgroundColor: CORES.fundo, borderTopColor: CORES.cartao, height: 64, paddingBottom: 8 },
        tabBarActiveTintColor: CORES.destaque,
        tabBarInactiveTintColor: CORES.textoFraco,
        tabBarLabelStyle: { fontSize: 12, fontWeight: '600' },
      }}
    >
      <Tab.Screen name="Painel" component={DashboardScreen} options={{ tabBarIcon: icone('📊') }} />
      <Tab.Screen name="Alertas" component={ListaAlertasScreen} options={{ tabBarIcon: icone('⚠️') }} />
      <Tab.Screen name="Reportar" component={ReportarScreen} options={{ tabBarIcon: icone('📝') }} />
      <Tab.Screen
        name="Avisos"
        component={NotificacoesScreen}
        options={{ tabBarIcon: icone('🔔'), tabBarBadge: naoLidos > 0 ? naoLidos : undefined }}
      />
      <Tab.Screen name="Perfil" component={PerfilScreen} options={{ tabBarIcon: icone('👤') }} />
    </Tab.Navigator>
  );
}

function Raiz() {
  const { pronto, preferencias } = useRisk();
  if (!pronto) {
    return (
      <View style={{ flex: 1, backgroundColor: CORES.fundo, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator size="large" color={CORES.destaque} />
      </View>
    );
  }
  return (
    <>
      <NavigationContainer ref={navegacaoRef} theme={tema}>
        <Stack.Navigator
          initialRouteName={preferencias.lgpdAceito ? 'MainTabs' : 'Onboarding'}
          screenOptions={{ headerStyle: { backgroundColor: CORES.fundo }, headerTintColor: CORES.texto }}
        >
          <Stack.Screen name="Onboarding" component={OnboardingScreen} options={{ headerShown: false }} />
          <Stack.Screen name="MainTabs" component={MainTabs} options={{ headerShown: false }} />
          <Stack.Screen name="DetalheAlerta" component={DetalheAlertaScreen} options={{ title: 'Detalhes do alerta' }} />
          <Stack.Screen name="Transparencia" component={TransparenciaScreen} options={{ title: 'Fontes, confiança e IA' }} />
          <Stack.Screen name="Mapa" component={MapaScreen} options={{ title: 'Mapa de riscos' }} />
        </Stack.Navigator>
      </NavigationContainer>
      <AlertaToast />
    </>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <RiskProvider>
        <StatusBar style="light" />
        <Raiz />
      </RiskProvider>
    </SafeAreaProvider>
  );
}
