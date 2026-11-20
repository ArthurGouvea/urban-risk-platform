import React, { useState } from 'react';
import { ScrollView, Text } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { MapaRiscos } from '../components/MapaRiscos';
import { AlertaCard } from '../components/AlertaCard';
import { estilos } from '../components/estilos';

export default function MapaScreen({ navigation }) {
  const { alertas } = useRisk();
  const [selecionado, setSelecionado] = useState(null);
  const atual = selecionado && alertas.find((a) => a.id === selecionado.id);

  return (
    <Tela comCabecalho>
      <ScrollView contentContainerStyle={estilos.scroll}>
        <Text style={estilos.subtitulo}>Toque em um ponto para ver o evento. Cor = nível de risco, tamanho = prioridade da IA.</Text>
        <MapaRiscos alertas={alertas} altura={380} onSelecionar={setSelecionado} />
        {atual ? (
          <>
            <Text style={estilos.secao}>Evento selecionado</Text>
            <AlertaCard alerta={atual} onPress={() => navigation.navigate('DetalheAlerta', { id: atual.id, alerta: atual })} />
          </>
        ) : (
          <Text style={[estilos.meta, { textAlign: 'center', marginTop: 12 }]}>{alertas.length} evento(s) ativo(s) no mapa</Text>
        )}
      </ScrollView>
    </Tela>
  );
}
