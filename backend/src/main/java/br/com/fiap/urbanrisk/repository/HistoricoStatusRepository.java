package br.com.fiap.urbanrisk.repository;

import br.com.fiap.urbanrisk.model.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findByAlertaIdOrderByIdAsc(Long alertaId);
}
