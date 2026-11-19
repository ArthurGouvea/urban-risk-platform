package br.com.fiap.urbanrisk.controller;

import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.service.PriorizacaoRiskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AlertaController {

    private final PriorizacaoRiskService service;

    public AlertaController(PriorizacaoRiskService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of("status", "ONLINE", "plataforma", "Urban Risk Intelligence");
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaRisco>> listarAlertas() {
        return ResponseEntity.ok(service.listarAlertasPriorizados());
    }

    @GetMapping("/alertas/{id}")
    public ResponseEntity<AlertaRisco> detalharAlerta(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping("/alertas")
    public ResponseEntity<AlertaRisco> registrarEvento(@RequestBody AlertaRisco novoAlerta) {
        return ResponseEntity.ok(service.processarNovoEvento(novoAlerta));
    }
}
