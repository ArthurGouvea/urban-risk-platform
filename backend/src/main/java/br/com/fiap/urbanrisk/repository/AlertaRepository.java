package br.com.fiap.urbanrisk.repository;

import br.com.fiap.urbanrisk.model.AlertaRisco;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertaRepository extends JpaRepository<AlertaRisco, Long> {
    List<AlertaRisco> findAllByOrderByScorePrioridadeDescDataHoraDesc();
    long countByBairroIgnoreCaseAndCategoriaIgnoreCase(String bairro, String categoria);
}