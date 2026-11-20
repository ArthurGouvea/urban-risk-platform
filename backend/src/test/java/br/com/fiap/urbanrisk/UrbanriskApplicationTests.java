package br.com.fiap.urbanrisk;

import br.com.fiap.urbanrisk.dto.LeituraSensorRequest;
import br.com.fiap.urbanrisk.dto.NovoEventoRequest;
import br.com.fiap.urbanrisk.exception.TransicaoInvalidaException;
import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.model.CicloVidaEvento;
import br.com.fiap.urbanrisk.model.HistoricoStatus;
import br.com.fiap.urbanrisk.model.TipoSensor;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import br.com.fiap.urbanrisk.service.IngestaoSensorService;
import br.com.fiap.urbanrisk.service.PriorizacaoRiskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes de integração do pipeline completo (banco H2 + IA), com o simulador desligado. */
@SpringBootTest(properties = "urbanrisk.simulador.habilitado=false")
class UrbanriskApplicationTests {

	@Autowired
	private PriorizacaoRiskService service;

	@Autowired
	private IngestaoSensorService ingestao;

	@Autowired
	private AlertaRepository alertaRepository;

	private NovoEventoRequest relato(String titulo, String descricao, String categoria, String bairro, String dispositivo) {
		return new NovoEventoRequest(titulo, descricao, categoria, bairro, -23.561437, -46.625312,
				"REGISTRO_COMUNITARIO", dispositivo);
	}

	@Test
	void leituraAcimaDoLimiarGeraAlertaCriticoDeSensor() {
		ingestao.registrarLeitura(new LeituraSensorRequest("TESTE-PLUV-A", TipoSensor.PLUVIOMETRO, "Bairro Teste A", -23.5, -46.6, 60.0));

		AlertaRisco alerta = alertaRepository.findFirstBySensorIdAndStatusNotInOrderByDataHoraDesc("TESTE-PLUV-A", CicloVidaEvento.INATIVOS).orElseThrow();
		assertEquals("SENSOR_IOT", alerta.getOrigemDado());
		assertEquals("ALTA", alerta.getNivelConfianca());
		assertTrue(alerta.getPercentualConfianca() >= 90);
		assertEquals("CRITICO", alerta.getNivelRisco());
		assertEquals("ALERTA_EMITIDO", alerta.getStatus());
		assertFalse(alerta.getFatoresIa().isEmpty());
	}

	@Test
	void leituraNormalNaoGeraAlerta() {
		ingestao.registrarLeitura(new LeituraSensorRequest("TESTE-TERM-N", TipoSensor.TERMOMETRO, "Bairro Teste N", -23.5, -46.6, 27.0));
		assertTrue(alertaRepository.findFirstBySensorIdAndStatusNotInOrderByDataHoraDesc("TESTE-TERM-N", CicloVidaEvento.INATIVOS).isEmpty());
	}

	@Test
	void relatoComunitarioIsoladoFicaEmValidacaoEAnonimizado() {
		AlertaRisco salvo = service.registrarEvento(relato("Situação estranha", "Algo diferente acontecendo", "FALTA_AGUA", "Bairro Teste B", "device-b"));

		assertEquals("REGISTRO_COMUNITARIO", salvo.getOrigemDado());
		assertTrue(salvo.getPercentualConfianca() < 60);
		assertEquals("EM_VALIDACAO", salvo.getStatus());
		assertTrue(salvo.getAutorAnonimizado().startsWith("USR-"));
		assertFalse(salvo.getAutorAnonimizado().contains("device-b"));
	}

	@Test
	void relatoCorroboradoPorSensorGanhaConfianca() {
		ingestao.registrarLeitura(new LeituraSensorRequest("TESTE-PLUV-C", TipoSensor.PLUVIOMETRO, "Bairro Teste C", -23.5, -46.6, 55.0));

		AlertaRisco salvo = service.registrarEvento(relato("Rua alagada", "Água entrando nas casas, enchente forte", "ALAGAMENTO", "Bairro Teste C", "device-c"));

		assertTrue(salvo.getPercentualConfianca() >= 80, "confiança = " + salvo.getPercentualConfianca());
		assertTrue(salvo.getJustificativasConfianca().stream().anyMatch(j -> j.contains("sensor")));
		assertTrue(salvo.getEventosCorrelacionados() >= 1);
	}

	@Test
	void iaSugereCategoriaQuandoTextoNaoCombina() {
		AlertaRisco salvo = service.registrarEvento(relato("Sem luz", "Fio caído no poste soltando faíscas, apagão", "ALAGAMENTO", "Bairro Teste D", "device-d"));
		assertEquals("FALTA_ENERGIA", salvo.getCategoriaSugeridaIa());
		assertTrue(salvo.getExplicacaoIa().contains("sugere a categoria FALTA_ENERGIA"));
	}

	@Test
	void dadosPessoaisSaoRemovidosAntesDePersistir() {
		AlertaRisco salvo = service.registrarEvento(relato("Alagamento", "Rua alagada, me liga (11) 98765-4321", "ALAGAMENTO", "Bairro Teste E", "device-e"));
		assertFalse(salvo.getDescricao().contains("98765"));
		assertEquals(-23.561, salvo.getLatitude().doubleValue(), 1e-9);
	}

	@Test
	void listaVemOrdenadaPorPrioridade() {
		List<AlertaRisco> alertas = service.listarAlertasPriorizados();
		assertNotNull(alertas);
		assertFalse(alertas.isEmpty());
		for (int i = 1; i < alertas.size(); i++) {
			assertTrue(alertas.get(i - 1).getScorePrioridade() >= alertas.get(i).getScorePrioridade());
		}
	}

	@Test
	void historicoRegistraFluxoReportadoConfirmadoAlertaEmitido() {
		ingestao.registrarLeitura(new LeituraSensorRequest("TESTE-PLUV-H", TipoSensor.PLUVIOMETRO, "Bairro Teste H", -23.5, -46.6, 70.0));
		AlertaRisco alerta = alertaRepository.findFirstBySensorIdAndStatusNotInOrderByDataHoraDesc("TESTE-PLUV-H", CicloVidaEvento.INATIVOS).orElseThrow();

		List<String> estados = service.historico(alerta.getId()).stream().map(HistoricoStatus::getStatusNovo).toList();
		assertEquals(List.of("REPORTADO", "CONFIRMADO", "ALERTA_EMITIDO"), estados);
	}

	@Test
	void agenteConfirmaRelatoEmValidacao() {
		AlertaRisco relato = service.registrarEvento(relato("Algo estranho", "Situação diferente", "FALTA_AGUA", "Bairro Teste F", "device-f"));
		assertEquals("EM_VALIDACAO", relato.getStatus());
		assertTrue(relato.getAcoesDisponiveis().contains("CONFIRMAR"));

		AlertaRisco confirmado = service.executarAcao(relato.getId(), CicloVidaEvento.Acao.CONFIRMAR, null, "Defesa Civil");

		assertTrue(confirmado.getPercentualConfianca() >= 90);
		assertTrue(List.of("CONFIRMADO", "ALERTA_EMITIDO").contains(confirmado.getStatus()));
		assertTrue(service.historico(relato.getId()).stream().anyMatch(h -> h.getResponsavel().startsWith("AGENTE")));
	}

	@Test
	void eventoDescartadoNaoAceitaNovasTransicoesENaoApareceNaLista() {
		AlertaRisco relato = service.registrarEvento(relato("Teste", "Situação diferente", "FALTA_AGUA", "Bairro Teste G", "device-g"));
		service.executarAcao(relato.getId(), CicloVidaEvento.Acao.DESCARTAR, "Trote", "Defesa Civil");

		assertThrows(TransicaoInvalidaException.class,
				() -> service.executarAcao(relato.getId(), CicloVidaEvento.Acao.CONFIRMAR, null, "Defesa Civil"));
		assertTrue(service.listarAlertasPriorizados().stream().noneMatch(a -> a.getId().equals(relato.getId())));
	}
}
