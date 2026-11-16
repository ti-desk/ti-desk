package mx.edu.tidesk.ti_desk.activo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivoRepository extends JpaRepository<Activo, Long>, JpaSpecificationExecutor<Activo> {

    boolean existsByEtiquetaInventario(String etiquetaInventario);

    boolean existsByEtiquetaInventarioAndIdNot(String etiquetaInventario, Long id);

    Optional<Activo> findByEtiquetaInventario(String etiquetaInventario);

    // Busqueda por etiqueta o numero de serie (RF-13)
    @Query("""
           SELECT a FROM Activo a
           WHERE LOWER(a.etiquetaInventario) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(a.numeroSerie) LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    Page<Activo> buscar(@Param("q") String q, Pageable pageable);
}