package br.com.fiap.urbanrisk.config;

import br.com.fiap.urbanrisk.dto.NovoEventoRequest;
import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import br.com.fiap.urbanrisk.service.PriorizacaoRiskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Carga inicial com eventos das três origens. Todos passam pelo pipeline de IA,
 * então confiança, score e explicações são calculados (nada é "chumbado").
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(AlertaRepository repository, PriorizacaoRiskService service) {
        return args -> {
            if (repository.count() > 0) return;
            LocalDateTime agora = LocalDateTime.now();

            // Sensores IoT (fixos, fora da rede simulada)
            service.avaliarESalvar(sensor("Alagamento severo na Av. do Estado (48.0 mm/h)",
                    "Pluviômetro IOT-PLUV-04 registrou 48 mm/h e lâmina d'água de 45 cm na pista local.",
                    "ALAGAMENTO", "Cambuci", -23.5614, -46.6253, "IOT-PLUV-04", 48.0, "mm/h", 7.4, 1.0,
                    agora.minusMinutes(12)));
            service.avaliarESalvar(sensor("Ilha de calor extrema (38.5 °C)",
                    "Termômetro IOT-TERM-09 registrou 38.5 °C em área de baixa arborização.",
                    "ILHA_DE_CALOR", "Brás", -23.5453, -46.6164, "IOT-TERM-09", 38.5, "°C", 5.1, 0.85,
                    agora.minusMinutes(40)));

            // Bases públicas
            service.registrarEvento(new NovoEventoRequest("Interrupção no fornecimento de energia",
                    "Queda de transformador afeta 3 quarteirões residenciais. Previsão de religamento em 2h.",
                    "FALTA_ENERGIA", "Vila Mariana", -23.5895, -46.6347, "BASE_PUBLICA", null), agora.minusMinutes(25));
            service.registrarEvento(new NovoEventoRequest("Baixa pressão no abastecimento de água",
                    "Manutenção emergencial na rede reduz a pressão até as 18h.",
                    "FALTA_AGUA", "Mooca", -23.5605, -46.5973, "BASE_PUBLICA", null), agora.minusMinutes(60));
            service.registrarEvento(new NovoEventoRequest("Linha 3-Vermelha com velocidade reduzida",
                    "Falha na sinalização entre Sé e Brás: trens com intervalos maiores e plataformas cheias.",
                    "TRANSPORTE", "Sé", -23.5503, -46.6339, "BASE_PUBLICA", null), agora.minusMinutes(18));

            // Registros comunitários
            service.registrarEvento(new NovoEventoRequest("Acúmulo de água na saída do metrô",
                    "Bueiro entupido formando poça na calçada.",
                    "ALAGAMENTO", "Pinheiros", -23.5672, -46.7019, "REGISTRO_COMUNITARIO", "seed-device-1"), agora.minusMinutes(50));
            service.registrarEvento(new NovoEventoRequest("Água entrando nas casas da Rua Lavapés",
                    "Água subindo rápido e idosos presos dentro de casa precisando de ajuda.",
                    "ALAGAMENTO", "Cambuci", -23.5632, -46.6241, "REGISTRO_COMUNITARIO", "seed-device-2"), agora.minusMinutes(8));
        };
    }

    private static AlertaRisco sensor(String titulo, String descricao, String categoria, String bairro,
                                      double lat, double lon, String sensorId, double valor, String unidade,
                                      double z, double intensidade, LocalDateTime dataHora) {
        AlertaRisco a = new AlertaRisco();
        a.setTitulo(titulo);
        a.setDescricao(descricao);
        a.setCategoria(categoria);
        a.setBairro(bairro);
        a.setLatitude(lat);
        a.setLongitude(lon);
        a.setOrigemDado(PriorizacaoRiskService.SENSOR_IOT);
        a.setSensorId(sensorId);
        a.setAutorAnonimizado(sensorId);
        a.setValorMedido(valor);
        a.setUnidadeMedida(unidade);
        a.setEscoreZ(z);
        a.setIntensidadeAnomalia(intensidade);
        a.setUrgenciaIa(0.5);
        a.setDataHora(dataHora);
        a.setAtualizadoEm(dataHora);
        return a;
    }
}
