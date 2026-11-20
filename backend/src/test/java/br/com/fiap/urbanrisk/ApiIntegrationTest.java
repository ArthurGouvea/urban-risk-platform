package br.com.fiap.urbanrisk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa a API REST real (servidor em porta aleatória) com o cliente HTTP do próprio JDK. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "urbanrisk.simulador.habilitado=false")
class ApiIntegrationTest {

	@Value("${local.server.port}")
	private int porta;

	private final HttpClient http = HttpClient.newHttpClient();

	private HttpResponse<String> get(String caminho) throws Exception {
		return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String caminho, String json) throws Exception {
		return post(caminho, json, null);
	}

	private HttpResponse<String> post(String caminho, String json, String token) throws Exception {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json));
		if (token != null) b.header("X-Token-Agente", token);
		return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
	}

	private long criarRelatoEmValidacao() throws Exception {
		var r = post("/api/alertas", "{\"titulo\":\"Estranho\",\"descricao\":\"Situação diferente\",\"categoria\":\"FALTA_AGUA\",\"bairro\":\"Bairro API 2\"}");
		assertEquals(201, r.statusCode());
		var m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(r.body());
		assertTrue(m.find());
		return Long.parseLong(m.group(1));
	}

	@Test
	void healthResponde() throws Exception {
		var r = get("/api/health");
		assertEquals(200, r.statusCode());
		assertTrue(r.body().contains("ONLINE"));
	}

	@Test
	void listaAlertasComOrigemConfiancaEFatoresDaIa() throws Exception {
		var r = get("/api/alertas");
		assertEquals(200, r.statusCode());
		assertTrue(r.body().startsWith("["));
		assertTrue(r.body().contains("origemDado"));
		assertTrue(r.body().contains("percentualConfianca"));
		assertTrue(r.body().contains("fatoresIa"));
	}

	@Test
	void alertaInexistenteRetorna404() throws Exception {
		assertEquals(404, get("/api/alertas/999999").statusCode());
	}

	@Test
	void relatoInvalidoRetorna400ComCampos() throws Exception {
		var r = post("/api/alertas", "{\"titulo\":\"\",\"categoria\":\"TERREMOTO\"}");
		assertEquals(400, r.statusCode());
		assertTrue(r.body().contains("campos"));
	}

	@Test
	void relatoValidoRetorna201EIgnoraScoreEnviadoPeloCliente() throws Exception {
		var r = post("/api/alertas", """
				{"titulo":"Rua alagada","descricao":"Água subindo rápido","categoria":"ALAGAMENTO",
				 "bairro":"Bairro API","origemDado":"REGISTRO_COMUNITARIO","dispositivoId":"abc",
				 "scorePrioridade":100}
				""");
		assertEquals(201, r.statusCode());
		assertTrue(r.body().contains("\"fatoresIa\""));
		assertTrue(r.body().contains("USR-"));
	}

	@Test
	void endpointsDeIaEPainel() throws Exception {
		assertEquals(200, get("/api/ia/modelo").statusCode());
		assertEquals(200, get("/api/painel/resumo").statusCode());
		assertEquals(200, get("/api/sensores/leituras/recentes").statusCode());
		var r = post("/api/ia/analisar-texto", "{\"texto\":\"fio caído soltando faíscas\",\"categoria\":\"ALAGAMENTO\"}");
		assertEquals(200, r.statusCode());
		assertTrue(r.body().contains("FALTA_ENERGIA"));
	}

	@Test
	void acaoDeAgenteExigeToken() throws Exception {
		long id = criarRelatoEmValidacao();
		assertEquals(401, post("/api/alertas/" + id + "/acoes", "{\"acao\":\"CONFIRMAR\"}").statusCode());
		assertEquals(401, post("/api/alertas/" + id + "/acoes", "{\"acao\":\"CONFIRMAR\"}", "errado").statusCode());
		var ok = post("/api/alertas/" + id + "/acoes", "{\"acao\":\"CONFIRMAR\",\"perfil\":\"Defesa Civil\"}", "defesa-civil-2026");
		assertEquals(200, ok.statusCode());
		assertTrue(get("/api/alertas/" + id + "/historico").body().contains("AGENTE"));
	}

	@Test
	void transicaoInvalidaRetorna409() throws Exception {
		long id = criarRelatoEmValidacao();
		var r = post("/api/alertas/" + id + "/acoes", "{\"acao\":\"MONITORAR\"}", "defesa-civil-2026");
		assertEquals(409, r.statusCode());
	}

	@Test
	void boletimDeBasePublicaExigeToken() throws Exception {
		String boletim = "{\"titulo\":\"Linha parada\",\"descricao\":\"Metrô parado\",\"categoria\":\"TRANSPORTE\",\"bairro\":\"Sé\",\"origemDado\":\"BASE_PUBLICA\"}";
		assertEquals(401, post("/api/alertas", boletim).statusCode());
		assertEquals(201, post("/api/alertas", boletim, "defesa-civil-2026").statusCode());
	}
}
