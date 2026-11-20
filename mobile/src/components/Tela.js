import React from 'react';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ConnectionBanner } from './ConnectionBanner';
import { estilos } from './estilos';

/** Estrutura padrão das telas: área segura + faixa de conexão. */
export function Tela({ children, comCabecalho = false }) {
  return (
    <SafeAreaView style={estilos.container} edges={comCabecalho ? ['left', 'right'] : ['top', 'left', 'right']}>
      <ConnectionBanner />
      {children}
    </SafeAreaView>
  );
}
