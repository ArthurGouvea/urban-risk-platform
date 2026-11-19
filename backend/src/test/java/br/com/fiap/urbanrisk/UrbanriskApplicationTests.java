package br.com.fiap.urbanrisk;

import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.service.PriorizacaoRiskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UrbanriskApplicationTests {

	@Autowired
	private PriorizacaoRiskService service;

	@Test
	void devePriorizarSensorIoTComAltaConfianca() {
		AlertaRisco alerta = new AlertaRisco();
		alerta.setTitulo("Alagamento Teste IoT");
		alerta.setCategoria("ALAGAMENTO");
		alerta.setBairro("Centro");
		alerta.setOrigemDado("SENSOR_IOT");

		AlertaRisco salvo = service.processarNovoEvento(alerta);

		assertEquals("ALTA", salvo.getNivelConfianca());
		assertTrue(salvo.getPercentualConfianca() >= 90);
		assertEquals("CRITICO", salvo.getNivelRisco());
		assertTrue(salvo.getScorePrioridade() >= 85);
	}

	@Test
	void deveAnonimizarAutorEmRegistroComunitarioLGPD() {
		AlertaRisco alerta = new AlertaRisco();
		alerta.setTitulo("Queda de Energia Teste");
		alerta.setCategoria("FALTA_ENERGIA");
		alerta.setBairro("Moema");
		alerta.setOrigemDado("REGISTRO_COMUNITARIO");

		AlertaRisco salvo = service.processarNovoEvento(alerta);

		assertNotNull(salvo.getAutorAnonimizado());
		assertTrue(salvo.getAutorAnonimizado().startsWith("USR-"));
	}
}