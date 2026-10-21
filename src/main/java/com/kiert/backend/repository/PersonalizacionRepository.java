// src/main/java/com/kiert/backend/repository/PersonalizacionRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.PersonalizacionUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalizacionRepository extends JpaRepository<PersonalizacionUsuario, Long> {

    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            LEFT JOIN FETCH p.usuario
            WHERE p.usuario.id = :usuarioId
            """)
    Optional<PersonalizacionUsuario> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(p) > 0 FROM PersonalizacionUsuario p
            WHERE p.usuario.id = :usuarioId
            """)
    boolean existeByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.marcoId = :marcoId
            ORDER BY p.fechaActualizacion DESC
            """)
    List<PersonalizacionUsuario> findByMarcoId(@Param("marcoId") String marcoId);

    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.fondoId = :fondoId
            ORDER BY p.fechaActualizacion DESC
            """)
    List<PersonalizacionUsuario> findByFondoId(@Param("fondoId") String fondoId);

    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.temaId = :temaId
            ORDER BY p.fechaActualizacion DESC
            """)
    List<PersonalizacionUsuario> findByTemaId(@Param("temaId") String temaId);

    @Query("""
            SELECT p.marcoId, COUNT(p)
            FROM PersonalizacionUsuario p
            WHERE p.marcoId IS NOT NULL
            GROUP BY p.marcoId
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarUsuariosPorMarco();

    @Query("""
            SELECT p.fondoId, COUNT(p)
            FROM PersonalizacionUsuario p
            WHERE p.fondoId IS NOT NULL
            GROUP BY p.fondoId
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarUsuariosPorFondo();

    @Query("""
            SELECT p.temaId, COUNT(p)
            FROM PersonalizacionUsuario p
            WHERE p.temaId IS NOT NULL
            GROUP BY p.temaId
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarUsuariosPorTema();

    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.marcoId = :marcoId
            """)
    long countByMarcoId(@Param("marcoId") String marcoId);

    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.fondoId = :fondoId
            """)
    long countByFondoId(@Param("fondoId") String fondoId);

    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.fotoPortadaUrl IS NOT NULL
            ORDER BY p.fechaActualizacion DESC
            """)
    Page<PersonalizacionUsuario> findConFotoPortada(Pageable pageable);

    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.marcoPersonalizadoUrl IS NOT NULL
            ORDER BY p.fechaActualizacion DESC
            """)
    Page<PersonalizacionUsuario> findConMarcoPersonalizado(Pageable pageable);

    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.temaId = 'default',
                p.marcoId = 'none',
                p.fondoId = 'default',
                p.marcoPersonalizadoUrl = null,
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.usuario.id = :usuarioId
            """)
    int resetearPersonalizacion(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.marcoId = 'none',
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.marcoId = :marcoId
            """)
    int desasignarMarcoDeUsuarios(@Param("marcoId") String marcoId);

    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.fondoId = 'default',
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.fondoId = :fondoId
            """)
    int desasignarFondoDeUsuarios(@Param("fondoId") String fondoId);

    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.temaId = 'default',
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.temaId = :temaId
            """)
    int desasignarTemaDeUsuarios(@Param("temaId") String temaId);

    @Modifying
    @Query("""
            DELETE FROM PersonalizacionUsuario p
            WHERE p.usuario IS NULL
            """)
    int eliminarHuerfanas();

    @Query("SELECT COUNT(p) FROM PersonalizacionUsuario p")
    long countTotal();

    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.temaId <> 'default'
               OR p.marcoId <> 'none'
               OR p.fondoId <> 'default'
            """)
    long countPersonalizados();
}