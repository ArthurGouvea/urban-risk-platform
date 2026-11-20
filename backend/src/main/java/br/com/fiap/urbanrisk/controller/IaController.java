package br.com.fiap.urbanrisk.controller;

import br.com.fiap.urbanrisk.dto.AnaliseTextoRequest;
import br.com.fiap.urbanrisk.ia.ClassificadorRelatos;
import br.com.fiap.urbanrisk.ia.DetectorAnomalias;
import br.com.fiap.urbanrisk.ia.MotorPriorizacao;
import br.com.fiap.urbanrisk.service.PriorizacaoRiskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ia")
public class IaController {

    private final PriorizacaoRiskService service;
    private final ClassificadorRelatos classificador;

    public IaController(PriorizacaoRiskService service, ClassificadorRelatos classificador) {
        this.service = service;
        this.classificador = classificador;
    }

    /** Analisa um texto livre: categoria provável, coerência com a categoria escolhida e urgência. */
    @PostMapping("/analisar-texto")
    public ResponseEntity<ClassificadorRelatos.AnaliseTexto> analisar(@Valid @RequestBody AnaliseTextoRequest req) {
        return ResponseEntity.ok(service.analisarTexto(req.texto(), req.categoria()));
    }

    /** Transparência: descreve os modelos e a qualidade medida por validação cruzada. */
    @GetMapping("/modelo")
    public ResponseEntity<Map<String, Object>> modelo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("nlp", Map.of(
                "algoritmo", "Naive Bayes multinomial (Laplace) + stemming por prefixo",
                "exemplosTreino", classificador.getTotalExemplos(),
                "vocabulario", classificador.getTamanhoVocabulario(),
                "acuraciaCategoriaLeaveOneOut", Math.round(classificador.getAcuraciaCategoria() * 1000) / 10.0,
                "acuraciaUrgenciaLeaveOneOut", Math.round(classificador.getAcuraciaUrgencia() * 1000) / 10.0));
        info.put("anomalias", Map.of(
                "algoritmo", "z-score em janela móvel (60 leituras) + limiares físicos",
                "limiarZ", DetectorAnomalias.Z_LIMIAR,
                "confirmacao", "2 leituras anômalas consecutivas ou limiar físico excedido"));
        info.put("priorizacao", Map.of(
                "algoritmo", "Regressão logística explicável",
                "intercepto", MotorPriorizacao.INTERCEPTO,
                "pesos", MotorPriorizacao.PESOS,
                "meiaVidaRecenciaMin", MotorPriorizacao.MEIA_VIDA_MIN,
                "faixas", "CRITICO>=85, ALTO>=70, MODERADO>=45, BAIXO<45"));
        return ResponseEntity.ok(info);
    }
}
