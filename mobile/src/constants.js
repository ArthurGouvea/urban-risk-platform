// Paleta, rótulos legíveis e metadados usados em todas as telas.

export const CORES = {
  fundo: '#0F172A',
  cartao: '#1E293B',
  borda: '#334155',
  texto: '#F8FAFC',
  textoSecundario: '#CBD5E1',
  textoFraco: '#94A3B8',
  destaque: '#38BDF8',
  primaria: '#2563EB',
  sucesso: '#16A34A',
  online: '#065F46',
  offline: '#B91C1C',
  reconectando: '#B45309',
};

export const NIVEIS = {
  CRITICO: { label: 'Crítico', cor: '#DC2626', ordem: 0 },
  ALTO: { label: 'Alto', cor: '#EA580C', ordem: 1 },
  MODERADO: { label: 'Moderado', cor: '#CA8A04', ordem: 2 },
  BAIXO: { label: 'Baixo', cor: '#16A34A', ordem: 3 },
};

export const CATEGORIAS = {
  ALAGAMENTO: { label: 'Alagamento', icone: '🌊' },
  ILHA_DE_CALOR: { label: 'Ilha de calor', icone: '🌡️' },
  FALTA_ENERGIA: { label: 'Falta de energia', icone: '⚡' },
  FALTA_AGUA: { label: 'Falta de água', icone: '🚰' },
  TRANSPORTE: { label: 'Transporte', icone: '🚇' },
};

export const ORIGENS = {
  SENSOR_IOT: { label: 'Sensor IoT', icone: '📡' },
  BASE_PUBLICA: { label: 'Base pública', icone: '🏛️' },
  REGISTRO_COMUNITARIO: { label: 'Relato comunitário', icone: '👥' },
};

// Ciclo de vida do evento de risco (mesma máquina de estados do backend)
export const STATUS = {
  REPORTADO: 'Reportado',
  EM_VALIDACAO: 'Em validação',
  CONFIRMADO: 'Confirmado',
  ALERTA_EMITIDO: 'Alerta emitido',
  EM_MONITORAMENTO: 'Em monitoramento',
  ENCERRADO: 'Encerrado',
  DESCARTADO: 'Descartado',
};

export const CORES_STATUS = {
  REPORTADO: '#64748B',
  EM_VALIDACAO: '#CA8A04',
  CONFIRMADO: '#2563EB',
  ALERTA_EMITIDO: '#DC2626',
  EM_MONITORAMENTO: '#7C3AED',
  ENCERRADO: '#16A34A',
  DESCARTADO: '#475569',
};

// Ações manuais de agente da Defesa Civil / operador de serviço
export const ACOES = {
  CONFIRMAR: { label: 'Confirmar (validado em campo)', icone: '✅' },
  EMITIR_ALERTA: { label: 'Emitir alerta', icone: '📢' },
  MONITORAR: { label: 'Colocar em monitoramento', icone: '👁️' },
  ENCERRAR: { label: 'Encerrar ocorrência', icone: '🏁' },
  DESCARTAR: { label: 'Descartar (falso/duplicado)', icone: '🗑️' },
};

// Personas (documento de requisitos)
export const PERFIS = [
  { id: 'Morador', icone: '🏠', descricao: 'Acompanhe riscos perto de você, receba avisos e envie relatos.', privilegiado: false },
  { id: 'Agente da Defesa Civil', icone: '🦺', descricao: 'Valide relatos, emita, monitore e encerre alertas.', privilegiado: true },
  { id: 'Operador de Serviço Público', icone: '🛠️', descricao: 'Publique boletins oficiais (energia, água, transporte) e atualize ocorrências.', privilegiado: true },
];

export const perfilPrivilegiado = (perfil) => !!(PERFIS.find((p) => p.id === perfil) || {}).privilegiado;

export const TIPOS_SENSOR = {
  PLUVIOMETRO: 'Chuva',
  NIVEL_CORREGO: 'Nível do córrego',
  TERMOMETRO: 'Temperatura',
  TENSAO_REDE: 'Tensão elétrica',
  PRESSAO_AGUA: 'Pressão da água',
};

export const rotulo = (mapa, chave) => (mapa[chave] && (mapa[chave].label || mapa[chave])) || chave || '—';
