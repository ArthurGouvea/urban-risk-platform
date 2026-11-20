package br.com.fiap.urbanrisk.model;

import br.com.fiap.urbanrisk.ia.DetectorAnomalias.Sentido;

/** Tipos de sensor suportados, com a categoria de risco, unidade, sentido de risco e limiar físico. */
public enum TipoSensor {
    PLUVIOMETRO("ALAGAMENTO", "mm/h", Sentido.ALTA, 30.0, 1.0, "Chuva intensa detectada"),
    NIVEL_CORREGO("ALAGAMENTO", "cm", Sentido.ALTA, 200.0, 5.0, "Nível do córrego acima do normal"),
    TERMOMETRO("ILHA_DE_CALOR", "°C", Sentido.ALTA, 36.0, 0.8, "Ilha de calor detectada"),
    TENSAO_REDE("FALTA_ENERGIA", "V", Sentido.BAIXA, 100.0, 2.0, "Queda de tensão na rede elétrica"),
    PRESSAO_AGUA("FALTA_AGUA", "bar", Sentido.BAIXA, 1.0, 0.08, "Queda de pressão no abastecimento");

    public final String categoria;
    public final String unidade;
    public final Sentido sentidoRisco;
    public final Double limiar;
    public final double desvioMinimo;
    public final String tituloAlerta;

    TipoSensor(String categoria, String unidade, Sentido sentidoRisco, Double limiar,
               double desvioMinimo, String tituloAlerta) {
        this.categoria = categoria;
        this.unidade = unidade;
        this.sentidoRisco = sentidoRisco;
        this.limiar = limiar;
        this.desvioMinimo = desvioMinimo;
        this.tituloAlerta = tituloAlerta;
    }
}
