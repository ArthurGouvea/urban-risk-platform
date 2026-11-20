package br.com.fiap.urbanrisk.repository;

import br.com.fiap.urbanrisk.model.AlertaRisco;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<AlertaRisco, Long> {

    /** Alertas ativos (status fora da lista informada), paginados/ordenados pelo Pageable. */
    List<AlertaRisco> findByStatusNotIn(Collection<String> status, Pageable pageable);

    List<AlertaRisco> findByStatusNotIn(Collection<String> status);

    /** Eventos no mesmo bairro/categoria desde um instante (correlação espacial-temporal), exceto descartados. */
    long countByBairroIgnoreCaseAndCategoriaIgnoreCaseAndDataHoraAfterAndStatusNot(
            String bairro, String categoria, LocalDateTime desde, String status);

    long countByBairroIgnoreCaseAndCategoriaIgnoreCaseAndOrigemDadoAndDataHoraAfterAndStatusNot(
            String bairro, String categoria, String origemDado, LocalDateTime desde, String status);

    long countByBairroIgnoreCaseAndCategoriaIgnoreCaseAndOrigemDadoAndStatusNotIn(
            String bairro, String categoria, String origemDado, Collection<String> status);

    Optional<AlertaRisco> findFirstBySensorIdAndStatusNotInOrderByDataHoraDesc(String sensorId, Collection<String> status);
}
