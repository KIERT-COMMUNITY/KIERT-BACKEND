package com.kiert.backend.repository;

import com.kiert.backend.entity.SolicitudContacto;
import com.kiert.backend.entity.SolicitudContacto.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    List<SolicitudContacto> findByReceptorIdAndEstado(Long receptorId, EstadoSolicitud estado);

    List<SolicitudContacto> findByEmisorIdAndEstado(Long emisorId, EstadoSolicitud estado);

    Optional<SolicitudContacto> findByEmisorIdAndReceptorIdAndEstado(
            Long emisorId, Long receptorId, EstadoSolicitud estado);

    boolean existsByEmisorIdAndReceptorIdAndEstado(
            Long emisorId, Long receptorId, EstadoSolicitud estado);
}