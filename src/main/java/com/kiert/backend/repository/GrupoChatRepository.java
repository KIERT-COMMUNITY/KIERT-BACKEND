package com.kiert.backend.repository;

import com.kiert.backend.entity.GrupoChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GrupoChatRepository extends JpaRepository<GrupoChat, Long> {

    // ✅ Grupos a los que pertenece un usuario (ACTIVO) O que creó
    @Query("SELECT DISTINCT g FROM GrupoChat g " +
            "WHERE g.activo = true AND (" +
            "  g.creador.id = :usuarioId " +
            "  OR EXISTS (SELECT 1 FROM MiembroGrupo m WHERE m.grupo = g AND m.usuario.id = :usuarioId AND m.estado = 'ACTIVO')" +
            ") " +
            "ORDER BY g.fechaCreacion DESC")
    List<GrupoChat> findGruposDeUsuario(@Param("usuarioId") Long usuarioId);

    // Grupos públicos para unirse
    @Query("SELECT g FROM GrupoChat g " +
            "WHERE g.tipo = 'PUBLICO' " +
            "AND g.activo = true " +
            "AND g.id NOT IN (" +
            "  SELECT m.grupo.id FROM MiembroGrupo m " +
            "  WHERE m.usuario.id = :usuarioId AND m.estado IN ('ACTIVO', 'PENDIENTE')" +
            ") " +
            "ORDER BY g.fechaCreacion DESC")
    List<GrupoChat> findGruposPublicosDisponibles(@Param("usuarioId") Long usuarioId);

    // Buscar grupos por nombre
    @Query("SELECT g FROM GrupoChat g " +
            "WHERE LOWER(g.nombre) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "AND g.activo = true " +
            "ORDER BY g.fechaCreacion DESC")
    List<GrupoChat> buscarPorNombre(@Param("query") String query);
}