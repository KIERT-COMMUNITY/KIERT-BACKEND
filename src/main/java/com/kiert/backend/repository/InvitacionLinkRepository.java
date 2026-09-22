// src/main/java/com/kiert/backend/repository/InvitacionLinkRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.InvitacionLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvitacionLinkRepository extends JpaRepository<InvitacionLink, Long> {

    Optional<InvitacionLink> findByToken(String token);

    @Query("SELECT i FROM InvitacionLink i " +
            "WHERE i.grupo.id = :grupoId AND i.activo = true " +
            "ORDER BY i.fechaCreacion DESC")
    List<InvitacionLink> findActivosByGrupo(@Param("grupoId") Long grupoId);

    @Query("SELECT COUNT(i) > 0 FROM InvitacionLink i " +
            "WHERE i.token = :token AND i.activo = true")
    boolean existeTokenActivo(@Param("token") String token);
}