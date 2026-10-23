package mx.edu.tidesk.ti_desk.activo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ActivoRepository extends JpaRepository<Activo, Long> {

    boolean existsByEtiquetaInventario(String etiquetaInventario);

    Optional<Activo> findByEtiquetaInventario(String etiquetaInventario);

    // BÃºsqueda por etiqueta o nÃºmero de serie (RF-13)
    @Query("""
           SELECT a FROM Activo a
           WHERE LOWER(a.etiquetaInventario) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(a.numeroSerie) LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    Page<Activo> buscar(@Param("q") String q, Pageable pageable);
}