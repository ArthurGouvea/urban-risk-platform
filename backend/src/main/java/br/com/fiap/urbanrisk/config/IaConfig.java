package br.com.fiap.urbanrisk.config;

import br.com.fiap.urbanrisk.ia.AvaliadorConfianca;
import br.com.fiap.urbanrisk.ia.ClassificadorRelatos;
import br.com.fiap.urbanrisk.ia.DetectorAnomalias;
import br.com.fiap.urbanrisk.ia.MotorPriorizacao;
import br.com.fiap.urbanrisk.privacidade.AnonimizadorLgpd;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

/** Registra os componentes de IA (Java puro, sem dependências externas) como beans do Spring. */
@Configuration
public class IaConfig {

    @Bean
    public ClassificadorRelatos classificadorRelatos() throws IOException {
        try (InputStream csv = IaConfig.class.getResourceAsStream("/ia/corpus-relatos.csv")) {
            if (csv == null) throw new IllegalStateException("Corpus de treino /ia/corpus-relatos.csv não encontrado");
            return ClassificadorRelatos.deCsv(csv);
        }
    }

    @Bean
    public DetectorAnomalias detectorAnomalias() {
        return new DetectorAnomalias();
    }

    @Bean
    public MotorPriorizacao motorPriorizacao() {
        return new MotorPriorizacao();
    }

    @Bean
    public AvaliadorConfianca avaliadorConfianca() {
        return new AvaliadorConfianca();
    }

    @Bean
    public AnonimizadorLgpd anonimizadorLgpd(@Value("${urbanrisk.lgpd.salt:troque-este-salt-em-producao}") String salt) {
        return new AnonimizadorLgpd(salt);
    }
}
