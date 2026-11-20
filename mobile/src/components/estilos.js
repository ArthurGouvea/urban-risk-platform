import { StyleSheet } from 'react-native';
import { CORES } from '../constants';

// Estilos compartilhados. Fontes >= 13 e áreas de toque >= 44 px (acessibilidade).
export const estilos = StyleSheet.create({
  container: { flex: 1, backgroundColor: CORES.fundo },
  scroll: { padding: 16, paddingBottom: 40 },
  tag: { color: CORES.destaque, fontWeight: 'bold', fontSize: 13, marginBottom: 4 },
  titulo: { color: CORES.texto, fontSize: 26, fontWeight: 'bold', marginBottom: 8 },
  cabecalho: { color: CORES.texto, fontSize: 22, fontWeight: 'bold', marginBottom: 4 },
  subtitulo: { color: CORES.textoFraco, fontSize: 14, marginBottom: 16, lineHeight: 20 },
  secao: { color: CORES.texto, fontSize: 17, fontWeight: 'bold', marginTop: 16, marginBottom: 8 },
  rotulo: { color: '#E2E8F0', fontSize: 15, fontWeight: '600', marginTop: 14, marginBottom: 6 },
  cartao: { backgroundColor: CORES.cartao, padding: 14, borderRadius: 12, marginBottom: 12 },
  cartaoTitulo: { color: CORES.texto, fontSize: 16, fontWeight: 'bold', marginVertical: 4 },
  corpo: { color: CORES.textoSecundario, fontSize: 14, lineHeight: 20 },
  meta: { color: CORES.textoFraco, fontSize: 13, marginTop: 4 },
  linha: { color: '#E2E8F0', fontSize: 14, marginVertical: 3, lineHeight: 20 },
  linhaHorizontal: { flexDirection: 'row', alignItems: 'center' },
  linhaQuebra: { flexDirection: 'row', flexWrap: 'wrap' },
  entreLados: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  chip: {
    backgroundColor: CORES.cartao, paddingHorizontal: 14, minHeight: 40, justifyContent: 'center',
    borderRadius: 20, marginRight: 8, marginBottom: 8, borderWidth: 1, borderColor: CORES.borda,
  },
  chipAtivo: { backgroundColor: CORES.primaria, borderColor: CORES.primaria },
  chipTexto: { color: CORES.textoSecundario, fontSize: 14, fontWeight: '600' },
  chipTextoAtivo: { color: '#FFFFFF' },
  input: {
    backgroundColor: CORES.cartao, color: CORES.texto, borderRadius: 10, padding: 12, fontSize: 15,
    borderWidth: 1, borderColor: CORES.borda, minHeight: 48,
  },
  botaoPrimario: { backgroundColor: CORES.primaria, padding: 15, borderRadius: 12, alignItems: 'center', marginTop: 18, minHeight: 50 },
  botaoPrimarioTexto: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 16 },
  botaoSecundario: { backgroundColor: CORES.borda, padding: 12, borderRadius: 10, alignItems: 'center', marginTop: 8, minHeight: 44 },
  botaoSecundarioTexto: { color: CORES.destaque, fontWeight: 'bold', fontSize: 14 },
  vazio: { color: CORES.textoFraco, fontSize: 15, textAlign: 'center', marginTop: 24, lineHeight: 22 },
});
