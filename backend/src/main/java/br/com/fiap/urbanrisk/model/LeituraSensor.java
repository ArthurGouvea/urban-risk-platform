package br.com.fiap.urbanrisk.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/** Leitura bruta de um sensor urbano (real ou simulado), já avaliada pelo detector de anomalias. */
@Entity
public class LeituraSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sensorId;
    private String tipo; // PLUVIOMETRO, NIVEL_CORREGO, TERMOMETRO, TENSAO_REDE, PRESSAO_AGUA
    private String bairro;
    private Double latitude;
    private Double longitude;
    private Double valor;
    private String unidade;
    private Double escoreZ;
    private Boolean anomalia;
    private LocalDateTime dataHora;

    public LeituraSensor() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public Double getEscoreZ() { return escoreZ; }
    public void setEscoreZ(Double escoreZ) { this.escoreZ = escoreZ; }
    public Boolean getAnomalia() { return anomalia; }
    public void setAnomalia(Boolean anomalia) { this.anomalia = anomalia; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
