package br.com.fiap.urbanrisk.service;

import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PriorizacaoRiskService {

    private final AlertaRepository repository;

    public PriorizacaoRiskService(AlertaRepository repository) {
        this.repository = repository;
    }

    public List<AlertaRisco> listarAlertasPriorizados() {
        return repository.findAllByOrderByScorePrioridadeDescDataHoraDesc();
    }

    public AlertaRisco buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
    }

    public AlertaRisco processarNovoEvento(AlertaRisco alerta) {
        alerta.setDataHora(LocalDateTime.now());

        // Proteção de Dados (LGPD): anonimiza identificação do cidadão
        alerta.setAutorAnonimizado("USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // Análise Inteligente: Cruzamento de ocorrências no mesmo bairro/categoria
        long ocorrenciasSimilares = repository.countByBairroIgnoreCaseAndCategoriaIgnoreCase(
                alerta.getBairro() != null ? alerta.getBairro() : "",
                alerta.getCategoria() != null ? alerta.getCategoria() : ""
        );

        // Cálculo de Nível de Confiança
        if ("SENSOR_IOT".equalsIgnoreCase(alerta.getOrigemDado()) || "BASE_PUBLICA".equalsIgnoreCase(alerta.getOrigemDado())) {
            alerta.setNivelConfianca("ALTA");
            alerta.setPercentualConfianca(95);
            alerta.setStatus("CONFIRMADO");
        } else {
            alerta.setOrigemDado("REGISTRO_COMUNITARIO");
            if (ocorrenciasSimilares >= 1) {
                // Validação cruzada: múltiplos relatos elevam a confiança
                alerta.setNivelConfianca("ALTA");
                alerta.setPercentualConfianca(88);
                alerta.setStatus("ALERTA_EMITIDO");
            } else {
                alerta.setNivelConfianca("MEDIA");
                alerta.setPercentualConfianca(62);
                alerta.setStatus("EM_VALIDACAO");
            }
        }

        // Cálculo do Score de Prioridade (0 a 100) baseado na gravidade + confiança
        int pesoCategoria = switch (alerta.getCategoria() != null ? alerta.getCategoria().toUpperCase() : "") {
            case "ALAGAMENTO" -> 45;
            case "FALTA_ENERGIA" -> 40;
            case "ILHA_DE_CALOR" -> 35;
            case "FALTA_AGUA" -> 30;
            default -> 25;
        };

        int score = Math.min(100, pesoCategoria + (alerta.getPercentualConfianca() / 2) + (int) (ocorrenciasSimilares * 10));
        alerta.setScorePrioridade(score);

        if (score >= 85) {
            alerta.setNivelRisco("CRITICO");
            alerta.setStatus("ALERTA_EMITIDO");
        } else if (score >= 70) {
            alerta.setNivelRisco("ALTO");
        } else if (score >= 50) {
            alerta.setNivelRisco("MODERADO");
        } else {
            alerta.setNivelRisco("BAIXO");
        }

        return repository.save(alerta);
    }
}