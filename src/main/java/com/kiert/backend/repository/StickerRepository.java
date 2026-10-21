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
            WHERE s.esPublico = true
              AND s.activo = true
            ORDER BY s.fechaCreacion DESC
            """)
    List<Sticker> findPublicos();

    /**
     * NUEVO: Obtiene un sticker activo por ID.
     */
    @Query("""
            SELECT s FROM Sticker s
            WHERE s.id = :id
              AND s.activo = true
            """)
    Optional<Sticker> findActivoById(@Param("id") Long id);

    /**
     * NUEVO: Cuenta stickers activos de un usuario.
     */
    @Query("""
            SELECT COUNT(s) FROM Sticker s
            WHERE s.usuario.id = :usuarioId
              AND s.activo = true
            """)
    long countActivosByUsuarioId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // SEGURIDAD: Validar permisos
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario es el dueño del sticker.
     * Se usa para impedir que un usuario modifique/elimine stickers ajenos.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM Sticker s
            WHERE s.id = :stickerId
              AND s.usuario.id = :usuarioId
              AND s.activo = true
            """)
    boolean esDuenoDelSticker(
            @Param("stickerId") Long stickerId,
            @Param("usuarioId") Long usuarioId
    );

    /**
     * NUEVO: Verifica si un usuario puede usar un sticker (es dueño o es público).
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM Sticker s
            WHERE s.id = :stickerId
              AND s.activo = true
              AND (s.usuario.id = :usuarioId OR s.esPublico = true)
            """)
    boolean puedeUsarSticker(
            @Param("stickerId") Long stickerId,
            @Param("usuarioId") Long usuarioId
    );
}