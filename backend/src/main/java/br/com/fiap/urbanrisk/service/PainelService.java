package br.com.fiap.urbanrisk.service;

import br.com.fiap.urbanrisk.dto.ResumoPainelResponse;
import br.com.fiap.urbanrisk.model.AlertaRisco;
import br.com.fiap.urbanrisk.model.CicloVidaEvento;
import br.com.fiap.urbanrisk.model.LeituraSensor;
import br.com.fiap.urbanrisk.repository.AlertaRepository;
import br.com.fiap.urbanrisk.repository.LeituraSensorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PainelService {

    private final AlertaRepository alertaRepository;
    private final LeituraSensorRepository leituraRepository;

    public PainelService(AlertaRepository alertaRepository, LeituraSensorRepository leituraRepository) {
        this.alertaRepository = alertaRepository;
        this.leituraRepository = leituraRepository;
    }

    public ResumoPainelResponse resumo() {
        List<AlertaRisco> ativos = alertaRepository.findByStatusNotIn(CicloVidaEvento.INATIVOS);
        LocalDateTime agora = LocalDateTime.now();
        return new ResumoPainelResponse(
                ativos.size(),
                contar(ativos, AlertaRisco::getNivelRisco),
                contar(ativos, AlertaRisco::getCategoria),
                contar(ativos, AlertaRisco::getOrigemDado),
                leituraRepository.countByDataHoraAfter(agora.minusMinutes(5)),
                leituraRepository.findFirstByOrderByDataHoraDesc().map(LeituraSensor::getDataHora).orElse(null),
                agora);
    }

    private static Map<String, Long> contar(List<AlertaRisco> alertas, Function<AlertaRisco, String> chave) {
        return alertas.stream()
                .filter(a -> chave.apply(a) != null)
                .collect(Collectors.groupingBy(chave, TreeMap::new, Collectors.counting()));
    }
}
