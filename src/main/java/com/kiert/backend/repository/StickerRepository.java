// src/main/java/com/kiert/backend/repository/StickerRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    @Query("""
            SELECT s FROM Sticker s
            WHERE s.usuario.id = :usuarioId
              AND s.activo = true
            ORDER BY s.fechaCreacion DESC
            """)
    List<Sticker> findByUsuarioIdOrderByFechaDesc(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT s FROM Sticker s
            WHERE s.id = :stickerId
              AND s.activo = true
              AND (s.esPublico = true OR s.usuario.id = :usuarioId)
            """)
    Optional<Sticker> findActivoDisponibleParaUsuario(
            @Param("stickerId") Long stickerId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT s FROM Sticker s
            WHERE s.esPublico = true
              AND s.activo = true
            ORDER BY s.fechaCreacion DESC
            """)
    List<Sticker> findPublicos();
}