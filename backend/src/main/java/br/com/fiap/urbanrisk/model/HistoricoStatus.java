package br.com.fiap.urbanrisk.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/** Registro de auditoria de cada transição de estado de um evento (linha do tempo exibida no app). */
@Entity
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long alertaId;
    private String statusAnterior;
    private String statusNovo;
    private String motivo;
    /** SISTEMA, IA ou o PERFIL do agente/operador (nunca dados pessoais). */
    private String responsavel;
    private LocalDateTime dataHora;

    public HistoricoStatus() {}

    public HistoricoStatus(Long alertaId, String statusAnterior, String statusNovo, String motivo,
                           String responsavel, LocalDateTime dataHora) {
        this.alertaId = alertaId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.motivo = motivo;
        this.responsavel = responsavel;
        this.dataHora = dataHora;
    }

    public Long getId() { return id; }
    public Long getAlertaId() { return alertaId; }
    public String getStatusAnterior() { return statusAnterior; }
    public String getStatusNovo() { return statusNovo; }
    public String getMotivo() { return motivo; }
    public String getResponsavel() { return responsavel; }
    public LocalDateTime getDataHora() { return dataHora; }
}
