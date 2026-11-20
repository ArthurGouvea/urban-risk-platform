import React, { useEffect, useMemo, useState } from 'react';
import { FlatList, RefreshControl, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useRisk } from '../context/RiskContext';
import { Tela } from '../components/Tela';
import { AlertaCard } from '../components/AlertaCard';
import { estilos } from '../components/estilos';
import { CATEGORIAS, CORES, NIVEIS, STATUS } from '../constants';

function Chips({ opcoes, valor, onChange, rotuloTodos = 'Todos' }) {
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ flexGrow: 0, flexShrink: 0, marginBottom: 2 }} contentContainerStyle={{ alignItems: 'center' }}>
      {[[null, rotuloTodos], ...opcoes].map(([chave, texto]) => {
        const ativo = valor === chave;
        return (
          <TouchableOpacity
            key={String(chave)}
            style={[estilos.chip, ativo && estilos.chipAtivo]}
            onPress={() => onChange(chave)}
            accessibilityRole="button"
            accessibilityState={{ selected: ativo }}
          >
            <Text style={[estilos.chipTexto, ativo && estilos.chipTextoAtivo]}>{texto}</Text>
          </TouchableOpacity>
        );
      })}
    </ScrollView>
  );
}

export default function ListaAlertasScreen({ navigation, route }) {
  const { alertas, sincronizarAgora } = useRisk();
  const [categoria, setCategoria] = useState(null);
  const [nivel, setNivel] = useState(null);
  const [bairro, setBairro] = useState(null);
  const [status, setStatus] = useState(null);

  // filtros vindos do Painel (toque em um nível, zona ou fila de validação)
  useEffect(() => {
    const p = route.params || {};
    if (p.nivel !== undefined || p.bairro !== undefined || p.status !== undefined) {
      setNivel(p.nivel ?? null);
      setBairro(p.bairro ?? null);
      setStatus(p.status ?? null);
      setCategoria(null);
      navigation.setParams({ nivel: undefined, bairro: undefined, status: undefined });
    }
  }, [route.params, navigation]);

  const filtrados = useMemo(() => alertas.filter((a) =>
    (!categoria || a.categoria === categoria) && (!nivel || a.nivelRisco === nivel)
    && (!bairro || a.bairro === bairro) && (!status || a.status === status)), [alertas, categoria, nivel, bairro, status]);

  return (
    <Tela>
      <View style={[estilos.scroll, { flex: 1, paddingBottom: 0 }]}>
        <Text style={estilos.cabecalho} accessibilityRole="header">Alertas priorizados</Text>
        <Text style={estilos.subtitulo}>Do mais urgente para o menos urgente • {filtrados.length} de {alertas.length}</Text>
        <Chips opcoes={Object.entries(CATEGORIAS).map(([k, v]) => [k, `${v.icone} ${v.label}`])} valor={categoria} onChange={setCategoria} rotuloTodos="Todas categorias" />
        <Chips opcoes={Object.entries(NIVEIS).map(([k, v]) => [k, v.label])} valor={nivel} onChange={setNivel} rotuloTodos="Todos níveis" />
        <Chips
          opcoes={['EM_VALIDACAO', 'CONFIRMADO', 'ALERTA_EMITIDO', 'EM_MONITORAMENTO'].map((k) => [k, STATUS[k]])}
          valor={status}
          onChange={setStatus}
          rotuloTodos="Todas situações"
        />
        {bairro && (
          <TouchableOpacity onPress={() => setBairro(null)} style={[estilos.chip, estilos.chipAtivo, { alignSelf: 'flex-start' }]} accessibilityRole="button">
            <Text style={[estilos.chipTexto, estilos.chipTextoAtivo]}>📍 {bairro}  ✕</Text>
          </TouchableOpacity>
        )}
        <FlatList
          data={filtrados}
          keyExtractor={(item) => String(item.id)}
          refreshControl={<RefreshControl refreshing={false} onRefresh={sincronizarAgora} tintColor={CORES.destaque} />}
          renderItem={({ item }) => (
            <AlertaCard alerta={item} onPress={() => navigation.navigate('DetalheAlerta', { id: item.id, alerta: item })} />
          )}
          ListEmptyComponent={<Text style={estilos.vazio}>Nenhum alerta com esses filtros.</Text>}
          contentContainerStyle={{ paddingBottom: 32 }}
        />
      </View>
    </Tela>
  );
}
