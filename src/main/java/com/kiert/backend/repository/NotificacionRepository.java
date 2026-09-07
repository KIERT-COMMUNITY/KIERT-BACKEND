// src/main/java/com/kiert/backend/repository/NotificacionRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("SELECT n FROM Notificacion n " +
            "LEFT JOIN FETCH n.usuarioOrigen " +
            "LEFT JOIN FETCH n.post " +
            "WHERE n.usuarioDestino.id = :usuarioId " +
            "ORDER BY n.fechaCreacion DESC")
    List<Notificacion> findByUsuarioDestinoIdOrderByFechaCreacionDesc(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false")
    long countNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP WHERE n.id IN :ids")
    void marcarComoLeidas(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false")
    void marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId);

    @Query("SELECT n FROM Notificacion n WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false")
    List<Notificacion> findNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);
}