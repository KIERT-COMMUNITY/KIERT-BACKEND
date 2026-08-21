package com.kiert.backend.repository;

import com.kiert.backend.entity.SolicitudContacto;
import com.kiert.backend.entity.SolicitudContacto.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findByReceptorIdAndEstado(Long receptorId, EstadoSolicitud estado);

    List<SolicitudContacto> findByEmisorIdAndEstado(Long emisorId, EstadoSolicitud estado);

    @Query("SELECT s FROM SolicitudContacto s WHERE (s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId) AND s.estado = :estado")
    List<SolicitudContacto> findAllByUsuarioIdAndEstado(@Param("usuarioId") Long usuarioId, @Param("estado") EstadoSolicitud estado);

    Optional<SolicitudContacto> findByEmisorIdAndReceptorIdAndEstado(
            Long emisorId, Long receptorId, EstadoSolicitud estado);

    boolean existsByEmisorIdAndReceptorIdAndEstado(
            Long emisorId, Long receptorId, EstadoSolicitud estado);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM SolicitudContacto s " +
            "WHERE ((s.emisor.id = :usuario1 AND s.receptor.id = :usuario2) OR " +
            "(s.emisor.id = :usuario2 AND s.receptor.id = :usuario1)) " +
            "AND s.estado = 'ACEPTADA'")
    boolean sonContactos(@Param("usuario1") Long usuario1, @Param("usuario2") Long usuario2);
}