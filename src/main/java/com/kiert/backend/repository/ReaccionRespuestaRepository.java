package com.kiert.backend.repository;

import com.kiert.backend.entity.ReaccionRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReaccionRespuestaRepository extends JpaRepository<ReaccionRespuesta, Long> {

    Optional<ReaccionRespuesta> findByUsuarioIdAndRespuestaId(Long usuarioId, Long respuestaId);

    @Query("SELECT r.tipo, COUNT(r) FROM ReaccionRespuesta r WHERE r.respuesta.id = :respuestaId GROUP BY r.tipo")
    List<Object[]> countReaccionesByRespuesta(@Param("respuestaId") Long respuestaId);

    long countByRespuestaId(Long respuestaId);
}