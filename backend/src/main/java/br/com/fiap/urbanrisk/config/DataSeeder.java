package br.com.fiap.urbanrisk.config;

import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(AlertaRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        new AlertaRisco(
                                "Alagamento Severo na Av. do Estado",
                                "Nível da água atingiu 45cm na pista local. Trânsito bloqueado.",
                                "ALAGAMENTO", "Cambuci", -23.5614, -46.6253,
                                "SENSOR_IOT", "ALTA", 97, "CRITICO", 95, "ALERTA_EMITIDO",
                                "IOT-PLUV-04", LocalDateTime.now().minusMinutes(12)
                        ),
                        new AlertaRisco(
                                "Interrupção no Fornecimento de Energia",
                                "Queda de transformador afeta 3 quarteirões residenciais.",
                                "FALTA_ENERGIA", "Vila Mariana", -23.5895, -46.6347,
                                "BASE_PUBLICA", "ALTA", 94, "ALTO", 84, "CONFIRMADO",
                                "API-CONCESSIONARIA", LocalDateTime.now().minusMinutes(25)
                        ),
                        new AlertaRisco(
                                "Ilha de Calor Extrema (38.5°C)",
                                "Sensação térmica crítica registrada em área de baixa arborização.",
                                "ILHA_DE_CALOR", "Brás", -23.5453, -46.6164,
                                "SENSOR_IOT", "ALTA", 92, "ALTO", 79, "ALERTA_EMITIDO",
                                "IOT-TERM-11", LocalDateTime.now().minusMinutes(40)
                        ),
                        new AlertaRisco(
                                "Acúmulo de Água na Saída do Metrô",
                                "Moradores relatam bueiro entupido causando lâmina d'água na calçada.",
                                "ALAGAMENTO", "Pinheiros", -23.5672, -46.7019,
                                "REGISTRO_COMUNITARIO", "MEDIA", 65, "MODERADO", 62, "EM_VALIDACAO",
                                "USR-9A4F21B0", LocalDateTime.now().minusMinutes(50)
                        ),
                        new AlertaRisco(
                                "Baixa Pressão no Abastecimento de Água",
                                "Manutenção emergencial na rede reduz pressão até as 18h.",
                                "FALTA_AGUA", "Mooca", -23.5605, -46.5973,
                                "BASE_PUBLICA", "ALTA", 96, "MODERADO", 58, "CONFIRMADO",
                                "API-SANEAMENTO", LocalDateTime.now().minusHours(1)
                        )
                ));
            }
        };
    }
}