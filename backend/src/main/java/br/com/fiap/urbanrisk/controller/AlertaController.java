package br.com.fiap.urbanrisk.controller;

import br.com.fiap.urbanrisk.dto.AcaoAgenteRequest;
import br.com.fiap.urbanrisk.dto.NovoEventoRequest;
import br.com.fiap.urbanrisk.dto.ResumoPainelResponse;
import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.model.HistoricoStatus;
import br.com.fiap.urbanrisk.seguranca.ControleAcesso;
import br.com.fiap.urbanrisk.service.PainelService;
import br.com.fiap.urbanrisk.service.PriorizacaoRiskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AlertaController {

    private final PriorizacaoRiskService service;
    private final PainelService painelService;
    private final ControleAcesso controleAcesso;

    public AlertaController(PriorizacaoRiskService service, PainelService painelService, ControleAcesso controleAcesso) {
        this.service = service;
        this.painelService = painelService;
        this.controleAcesso = controleAcesso;
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
                "status", "ONLINE",
                "plataforma", "Urban Risk Intelligence",
                "horarioServidor", LocalDateTime.now().toString());
    }

    /** Lista de alertas ativos já PRIORIZADA pela IA (maior score primeiro). */
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaRisco>> listarAlertas(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String nivelRisco,
            @RequestParam(required = false) String bairro,
            @RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(service.listarAlertasPriorizados(categoria, nivelRisco, bairro, limite));
    }

    @GetMapping("/alertas/{id}")
    public ResponseEntity<AlertaRisco> detalharAlerta(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /**
     * Relato comunitário (aberto) ou boletim de base pública (exige token de operador).
     * Score, confiança e estado são sempre calculados no servidor.
     */
    @PostMapping("/alertas")
    public ResponseEntity<AlertaRisco> registrarEvento(
            @Valid @RequestBody NovoEventoRequest req,
            @RequestHeader(value = ControleAcesso.CABECALHO, required = false) String token) {
        if ("BASE_PUBLICA".equalsIgnoreCase(req.origemDado())) controleAcesso.exigirAgente(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEvento(req));
    }

    /** Linha do tempo do evento (todas as transições de estado, com motivo e responsável). */
    @GetMapping("/alertas/{id}/historico")
    public ResponseEntity<List<HistoricoStatus>> historico(@PathVariable Long id) {
        return ResponseEntity.ok(service.historico(id));
    }

    /** Ação de agente da Defesa Civil / operador: CONFIRMAR, DESCARTAR, EMITIR_ALERTA, MONITORAR, ENCERRAR. */
    @PostMapping("/alertas/{id}/acoes")
    public ResponseEntity<AlertaRisco> executarAcao(
            @PathVariable Long id,
            @Valid @RequestBody AcaoAgenteRequest req,
            @RequestHeader(value = ControleAcesso.CABECALHO, required = false) String token) {
        controleAcesso.exigirAgente(token);
        return ResponseEntity.ok(service.executarAcao(id, req.acao(), req.motivo(), req.perfil()));
    }

    @GetMapping("/painel/resumo")
    public ResponseEntity<ResumoPainelResponse> resumo() {
        return ResponseEntity.ok(painelService.resumo());
    }
}
