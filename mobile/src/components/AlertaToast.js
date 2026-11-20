import React, { useEffect } from 'react';
import { StyleSheet, Text, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useRisk } from '../context/RiskContext';
import { NIVEIS } from '../constants';
import { textoNotificacao } from '../services/notificacoes';
import { abrirAlerta } from '../navegacao';

/** Aviso flutuante dentro do app quando chega um alerta novo/escalado (funciona também na web). */
export function AlertaToast() {
  const { toast, fecharToast } = useRisk();
  const insets = useSafeAreaInsets();

  useEffect(() => {
    if (!toast) return undefined;
    const t = setTimeout(fecharToast, 7000);
    return () => clearTimeout(t);
  }, [toast, fecharToast]);

  if (!toast) return null;

  if (toast.tipo === 'info') {
    return (
      <TouchableOpacity style={[s.toast, { top: insets.top + 8, backgroundColor: '#1D4ED8' }]} onPress={fecharToast} accessibilityRole="alert">
        <Text style={s.titulo}>{toast.texto}</Text>
      </TouchableOpacity>
    );
  }

  const { titulo, corpo } = textoNotificacao(toast.item);
  const cor = (NIVEIS[toast.item.alerta.nivelRisco] || {}).cor || '#DC2626';
  return (
    <TouchableOpacity
      style={[s.toast, { top: insets.top + 8, backgroundColor: cor }]}
      onPress={() => { abrirAlerta(toast.item.alerta.id); fecharToast(); }}
      accessibilityRole="alert"
      accessibilityLabel={`${titulo}. ${corpo}. Toque para ver detalhes.`}
    >
      <Text style={s.titulo}>🔔 {titulo}</Text>
      <Text style={s.corpo}>{corpo}{toast.extras > 0 ? `  (+${toast.extras} outro(s))` : ''}</Text>
      <Text style={s.dica}>Toque para ver detalhes</Text>
    </TouchableOpacity>
  );
}

const s = StyleSheet.create({
  toast: {
    position: 'absolute', left: 12, right: 12, padding: 14, borderRadius: 14, zIndex: 1000, elevation: 12,
    shadowColor: '#000', shadowOpacity: 0.4, shadowRadius: 8, shadowOffset: { width: 0, height: 4 },
  },
  titulo: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 15 },
  corpo: { color: '#FFFFFF', fontSize: 14, marginTop: 4 },
  dica: { color: 'rgba(255,255,255,0.85)', fontSize: 12, marginTop: 6 },
});
