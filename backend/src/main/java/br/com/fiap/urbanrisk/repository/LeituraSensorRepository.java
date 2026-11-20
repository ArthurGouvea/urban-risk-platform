package br.com.fiap.urbanrisk.repository;

import br.com.fiap.urbanrisk.model.LeituraSensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeituraSensorRepository extends JpaRepository<LeituraSensor, Long> {

    List<LeituraSensor> findTop30ByOrderByDataHoraDesc();

    Optional<LeituraSensor> findFirstByOrderByDataHoraDesc();

    long countByDataHoraAfter(LocalDateTime desde);

    long deleteByDataHoraBefore(LocalDateTime limite);
}
