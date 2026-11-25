package cl.duoc.ferreteria.ventas.repository;

import cl.duoc.ferreteria.ventas.model.Boleta;
import cl.duoc.ferreteria.ventas.model.EstadoBoleta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @EntityGraph trae los detalles en la MISMA consulta: con open-in-view=false,
 * leerlos fuera de la transacción daría LazyInitializationException, y sin él
 * serían N+1 consultas.
 */
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    @EntityGraph(attributePaths = "detalles")
    List<Boleta> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = "detalles")
    List<Boleta> findByEstadoOrderByFechaDesc(EstadoBoleta estado);

    @EntityGraph(attributePaths = "detalles")
    List<Boleta> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    @EntityGraph(attributePaths = "detalles")
    Optional<Boleta> findWithDetallesById(Long id);
}
