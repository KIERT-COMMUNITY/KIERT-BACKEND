// src/main/java/com/kiert/backend/repository/MensajeGlobalRepository.java
package com.kiert.backend.features.chatGlobal.repository;

import com.kiert.backend.entity.MensajeGlobal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeGlobalRepository extends JpaRepository<MensajeGlobal, Long> {

    @Query("""
            SELECT m FROM MensajeGlobal m
            JOIN FETCH m.autor
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGlobal> findUltimosConAutor(Pageable pageable);

    @Query("""
            SELECT m FROM MensajeGlobal m
            JOIN FETCH m.autor
            WHERE m.id < :antesDeId
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGlobal> findAnterioresConAutor(Long antesDeId, Pageable pageable);
}