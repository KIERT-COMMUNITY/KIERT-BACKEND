// src/main/java/com/kiert/backend/features/chatGlobal/repository/MensajeGlobalRepository.java
package com.kiert.backend.features.chatGlobal.repository;

import com.kiert.backend.entity.MensajeGlobal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeGlobalRepository extends JpaRepository<MensajeGlobal, Long> {

    @Query(value = """
            SELECT m FROM MensajeGlobal m
            JOIN FETCH m.autor a
            LEFT JOIN FETCH a.personalizacion
            ORDER BY m.fechaEnvio DESC
            """,
            countQuery = "SELECT COUNT(m) FROM MensajeGlobal m")
    Page<MensajeGlobal> findUltimosConAutor(Pageable pageable);

    @Query(value = """
            SELECT m FROM MensajeGlobal m
            JOIN FETCH m.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE m.id < :antesDeId
            ORDER BY m.fechaEnvio DESC
            """,
            countQuery = "SELECT COUNT(m) FROM MensajeGlobal m WHERE m.id < :antesDeId")
    Page<MensajeGlobal> findAnterioresConAutor(@Param("antesDeId") Long antesDeId, Pageable pageable);
}