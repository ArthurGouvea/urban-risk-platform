import { useEffect, useState } from 'react';

/** Re-renderiza o componente a cada `ms` para manter textos como "há 12 s" atualizados. */
export function useAgora(ms = 1000) {
  const [agora, setAgora] = useState(() => new Date());
  useEffect(() => {
    const t = setInterval(() => setAgora(new Date()), ms);
    return () => clearInterval(t);
  }, [ms]);
  return agora;
}
