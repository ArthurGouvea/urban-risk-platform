package br.com.fiap.urbanrisk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Evento/alerta de risco urbano. Cada registro informa a ORIGEM do dado, o NÍVEL DE CONFIANÇA
 * (com justificativas) e a PRIORIDADE calculada pelo motor de IA (com os fatores que a explicam).
 */
@Entity
public class AlertaRisco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    @Column(length = 1000)
    private String descricao;
    private String categoria; // ALAGAMENTO, ILHA_DE_CALOR, FALTA_ENERGIA, FALTA_AGUA
    private String bairro;
    private Double latitude;
    private Double longitude;

    // ---- Origem e confiança ----
    private String origemDado; // SENSOR_IOT, BASE_PUBLICA, REGISTRO_COMUNITARIO
    private String nivelConfianca; // ALTA, MEDIA, BAIXA
    private Integer percentualConfianca; // 0 a 100
    @Column(length = 2000)
    private String justificativaConfianca;

    // ---- Priorização (IA) ----
    private String nivelRisco; // CRITICO, ALTO, MODERADO, BAIXO
    private Integer scorePrioridade; // 0 a 100 (ordenação)
    private Integer probabilidadeRiscoIa; // % estimada pelo modelo logístico
    private String categoriaSugeridaIa; // categoria inferida pelo NLP (Naive Bayes)
    private Double urgenciaIa; // 0..1 (NLP)
    private Double coerenciaIa; // 0..1 (texto x categoria informada)
    private Double intensidadeAnomalia; // 0..1 (sensor)
    private Integer eventosCorrelacionados;
    @Column(length = 4000)
    private String explicacaoIa;

    // ---- Dados de sensor (quando a origem é SENSOR_IOT) ----
    private String sensorId;
    private Double valorMedido;
    private String unidadeMedida;
    private Double escoreZ;

    private String status; // ver CicloVidaEvento
    private LocalDateTime statusAlteradoEm;
    private Boolean validadoPorAgente;
    private String autorAnonimizado; // pseudônimo LGPD (nunca o identificador real)
    private LocalDateTime dataHora;
    private LocalDateTime atualizadoEm;

    public AlertaRisco() {}

    /** Fatores da IA em lista (usado pelo app para exibir "por que este alerta é prioritário"). */
    public List<String> getFatoresIa() {
        return explicacaoIa == null || explicacaoIa.isBlank() ? List.of() : Arrays.asList(explicacaoIa.split("\n"));
    }

    /** Ações manuais permitidas a partir do estado atual (usado pelo app do agente/operador). */
    public List<String> getAcoesDisponiveis() {
        return CicloVidaEvento.acoesDisponiveis(status);
    }

    public List<String> getJustificativasConfianca() {
        return justificativaConfianca == null || justificativaConfianca.isBlank()
                ? List.of() : Arrays.asList(justificativaConfianca.split("\n"));
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getOrigemDado() { return origemDado; }
    public void setOrigemDado(String origemDado) { this.origemDado = origemDado; }
    public String getNivelConfianca() { return nivelConfianca; }
    public void setNivelConfianca(String nivelConfianca) { this.nivelConfianca = nivelConfianca; }
    public Integer getPercentualConfianca() { return percentualConfianca; }
    public void setPercentualConfianca(Integer percentualConfianca) { this.percentualConfianca = percentualConfianca; }
    public String getJustificativaConfianca() { return justificativaConfianca; }
    public void setJustificativaConfianca(String justificativaConfianca) { this.justificativaConfianca = justificativaConfianca; }
    public String getNivelRisco() { return nivelRisco; }
    public void setNivelRisco(String nivelRisco) { this.nivelRisco = nivelRisco; }
    public Integer getScorePrioridade() { return scorePrioridade; }
    public void setScorePrioridade(Integer scorePrioridade) { this.scorePrioridade = scorePrioridade; }
    public Integer getProbabilidadeRiscoIa() { return probabilidadeRiscoIa; }
    public void setProbabilidadeRiscoIa(Integer probabilidadeRiscoIa) { this.probabilidadeRiscoIa = probabilidadeRiscoIa; }
    public String getCategoriaSugeridaIa() { return categoriaSugeridaIa; }
    public void setCategoriaSugeridaIa(String categoriaSugeridaIa) { this.categoriaSugeridaIa = categoriaSugeridaIa; }
    public Double getUrgenciaIa() { return urgenciaIa; }
    public void setUrgenciaIa(Double urgenciaIa) { this.urgenciaIa = urgenciaIa; }
    public Double getCoerenciaIa() { return coerenciaIa; }
    public void setCoerenciaIa(Double coerenciaIa) { this.coerenciaIa = coerenciaIa; }
    public Double getIntensidadeAnomalia() { return intensidadeAnomalia; }
    public void setIntensidadeAnomalia(Double intensidadeAnomalia) { this.intensidadeAnomalia = intensidadeAnomalia; }
    public Integer getEventosCorrelacionados() { return eventosCorrelacionados; }
    public void setEventosCorrelacionados(Integer eventosCorrelacionados) { this.eventosCorrelacionados = eventosCorrelacionados; }
    public String getExplicacaoIa() { return explicacaoIa; }
    public void setExplicacaoIa(String explicacaoIa) { this.explicacaoIa = explicacaoIa; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public Double getValorMedido() { return valorMedido; }
    public void setValorMedido(Double valorMedido) { this.valorMedido = valorMedido; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public Double getEscoreZ() { return escoreZ; }
    public void setEscoreZ(Double escoreZ) { this.escoreZ = escoreZ; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStatusAlteradoEm() { return statusAlteradoEm; }
    public void setStatusAlteradoEm(LocalDateTime statusAlteradoEm) { this.statusAlteradoEm = statusAlteradoEm; }
    public Boolean getValidadoPorAgente() { return validadoPorAgente; }
    public void setValidadoPorAgente(Boolean validadoPorAgente) { this.validadoPorAgente = validadoPorAgente; }
    public String getAutorAnonimizado() { return autorAnonimizado; }
    public void setAutorAnonimizado(String autorAnonimizado) { this.autorAnonimizado = autorAnonimizado; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
