package pe.com.honoriaexpress.stockflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.honoriaexpress.stockflow.entity.EncomiendaEvento;
import java.util.List;

public interface EncomiendaEventoRepository extends JpaRepository<EncomiendaEvento, Long> {
    List<EncomiendaEvento> findByEncomiendaIdOrderByIdAsc(Long id);
}
