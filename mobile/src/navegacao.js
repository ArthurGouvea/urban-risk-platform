import { createNavigationContainerRef } from '@react-navigation/native';

// Permite navegar a partir de componentes fora das telas (ex.: aviso flutuante de alerta).
export const navegacaoRef = createNavigationContainerRef();

export function abrirAlerta(id) {
  if (navegacaoRef.isReady()) navegacaoRef.navigate('DetalheAlerta', { id });
}
