package br.com.fiap.urbanrisk.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AlertaRisco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String descricao;
    private String categoria; // ALAGAMENTO, ILHA_DE_CALOR, FALTA_ENERGIA, FALTA_AGUA, TRANSPORTE
    private String bairro;
    private Double latitude;
    private Double longitude;

    private String origemDado; // SENSOR_IOT, BASE_PUBLICA, REGISTRO_COMUNITARIO
    private String nivelConfianca; // ALTA, MEDIA, BAIXA
    private Integer percentualConfianca; // 0 a 100

    private String nivelRisco; // CRITICO, ALTO, MODERADO, BAIXO
    private Integer scorePrioridade; // 0 a 100 (usado para ordenar)
    private String status; // REPORTADO, EM_VALIDACAO, CONFIRMADO, ALERTA_EMITIDO, ENCERRADO

    private String autorAnonimizado; // Conformidade LGPD
    private LocalDateTime dataHora;

    public AlertaRisco() {}

    public AlertaRisco(String titulo, String descricao, String categoria, String bairro,
                       Double latitude, Double longitude, String origemDado,
                       String nivelConfianca, Integer percentualConfianca,
                       String nivelRisco, Integer scorePrioridade, String status,
                       String autorAnonimizado, LocalDateTime dataHora) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.bairro = bairro;
        this.latitude = latitude;
        this.longitude = longitude;
        this.origemDado = origemDado;
        this.nivelConfianca = nivelConfianca;
        this.percentualConfianca = percentualConfianca;
        this.nivelRisco = nivelRisco;
        this.scorePrioridade = scorePrioridade;
        this.status = status;
        this.autorAnonimizado = autorAnonimizado;
        this.dataHora = dataHora;
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
    public String getNivelRisco() { return nivelRisco; }
    public void setNivelRisco(String nivelRisco) { this.nivelRisco = nivelRisco; }
    public Integer getScorePrioridade() { return scorePrioridade; }
    public void setScorePrioridade(Integer scorePrioridade) { this.scorePrioridade = scorePrioridade; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAutorAnonimizado() { return autorAnonimizado; }
    public void setAutorAnonimizado(String autorAnonimizado) { this.autorAnonimizado = autorAnonimizado; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}