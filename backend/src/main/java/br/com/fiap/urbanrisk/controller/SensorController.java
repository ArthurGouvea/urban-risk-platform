package br.com.fiap.urbanrisk.controller;

import br.com.fiap.urbanrisk.dto.LeituraSensorRequest;
import br.com.fiap.urbanrisk.model.LeituraSensor;
import br.com.fiap.urbanrisk.service.IngestaoSensorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sensores")
public class SensorController {

    private final IngestaoSensorService ingestao;

    public SensorController(IngestaoSensorService ingestao) {
        this.ingestao = ingestao;
    }

    /** Endpoint de ingestão para sensores IoT reais (o simulador usa o mesmo serviço). */
    @PostMapping("/leituras")
    public ResponseEntity<LeituraSensor> receberLeitura(@Valid @RequestBody LeituraSensorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestao.registrarLeitura(req));
    }

    /** Últimas leituras recebidas (feed em tempo real para o painel). */
    @GetMapping("/leituras/recentes")
    public ResponseEntity<List<LeituraSensor>> leiturasRecentes() {
        return ResponseEntity.ok(ingestao.leiturasRecentes());
    }
}
