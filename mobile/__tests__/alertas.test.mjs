import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  parseData, tempoRelativo, detectarNotificacoes, contarPorNivel, agruparPorBairro, gerarIdDispositivo,
} from '../src/utils/alertas.js';

test('parseData aceita ISO e array do Jackson', () => {
  assert.equal(parseData('2026-11-19T14:05:00').getHours(), 14);
  const d = parseData([2026, 11, 19, 14, 5, 30, 0]);
  assert.equal(d.getMonth(), 10);
  assert.equal(d.getSeconds(), 30);
  assert.equal(parseData(null), null);
  assert.equal(parseData('lixo'), null);
});

test('tempoRelativo', () => {
  const agora = new Date(2026, 10, 19, 14, 0, 0);
  assert.equal(tempoRelativo(new Date(2026, 10, 19, 13, 59, 58), agora), 'agora');
  assert.equal(tempoRelativo(new Date(2026, 10, 19, 13, 59, 30), agora), 'há 30 s');
  assert.equal(tempoRelativo(new Date(2026, 10, 19, 13, 50, 0), agora), 'há 10 min');
  assert.equal(tempoRelativo(new Date(2026, 10, 19, 11, 0, 0), agora), 'há 3 h');
});

test('primeira carga não notifica', () => {
  const { notificar, mapa } = detectarNotificacoes(null, [{ id: 1, nivelRisco: 'CRITICO' }]);
  assert.equal(notificar.length, 0);
  assert.equal(mapa.get(1), 'CRITICO');
});

test('notifica alertas novos relevantes e escalonamento para crítico', () => {
  const anterior = new Map([[1, 'MODERADO'], [2, 'ALTO']]);
  const { notificar } = detectarNotificacoes(anterior, [
    { id: 1, nivelRisco: 'CRITICO' }, // escalou
    { id: 2, nivelRisco: 'ALTO' }, // sem mudança
    { id: 3, nivelRisco: 'ALTO' }, // novo relevante
    { id: 4, nivelRisco: 'BAIXO' }, // novo irrelevante
  ]);
  assert.deepEqual(notificar.map((n) => [n.alerta.id, n.motivo]), [[1, 'ESCALOU'], [3, 'NOVO']]);
});

test('contagem e zonas por bairro', () => {
  const alertas = [
    { id: 1, bairro: 'Mooca', scorePrioridade: 50, nivelRisco: 'MODERADO', categoria: 'FALTA_AGUA' },
    { id: 2, bairro: 'Cambuci', scorePrioridade: 96, nivelRisco: 'CRITICO', categoria: 'ALAGAMENTO' },
    { id: 3, bairro: 'Mooca', scorePrioridade: 80, nivelRisco: 'ALTO', categoria: 'ALAGAMENTO' },
  ];
  assert.deepEqual(contarPorNivel(alertas), { CRITICO: 1, ALTO: 1, MODERADO: 1, BAIXO: 0 });
  const zonas = agruparPorBairro(alertas);
  assert.equal(zonas[0].bairro, 'Cambuci');
  assert.equal(zonas[1].total, 2);
  assert.equal(zonas[1].nivel, 'ALTO');
});

test('id de dispositivo é aleatório', () => {
  assert.notEqual(gerarIdDispositivo(), gerarIdDispositivo());
});
