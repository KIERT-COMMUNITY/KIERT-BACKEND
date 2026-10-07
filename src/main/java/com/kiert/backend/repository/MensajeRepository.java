package com.kiert.backend.repository;

import com.kiert.backend.entity.Mensaje;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    @EntityGraph(attributePaths = {"archivos"})
    @Query("""
            select distinct m from Mensaje m
            where ((m.emisor.id = :usuarioA and m.receptor.id = :usuarioB)
               or (m.emisor.id = :usuarioB and m.receptor.id = :usuarioA))
              and m.eliminado = false
            order by m.fechaEnvio asc
            """)
    List<Mensaje> findConversacion(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB
    );

    @EntityGraph(attributePaths = {"archivos"})
    @Query("""
            select distinct m from Mensaje m
            where (m.emisor.id = :usuarioId or m.receptor.id = :usuarioId)
              and m.eliminado = false
            order by m.fechaEnvio desc
            """)
    List<Mensaje> findTodosLosMensajesDeUsuario(
            @Param("usuarioId") Long usuarioId
    );

    long countByEmisorIdAndReceptorIdAndLeidoFalseAndEliminadoFalse(
            Long emisorId,
            Long receptorId
    );

    long countByReceptorIdAndLeidoFalseAndEliminadoFalse(Long receptorId);

    @Query("""
            select m from Mensaje m
            where m.emisor.id = :otroUsuarioId
              and m.receptor.id = :usuarioId
              and m.leido = false
              and m.eliminado = false
            order by m.fechaEnvio asc
            """)
    List<Mensaje> findConversacionNoLeidos(
            @Param("usuarioId") Long usuarioId,
            @Param("otroUsuarioId") Long otroUsuarioId
    );

    @Query("""
            select distinct
            case
                when m.emisor.id = :usuarioId then m.receptor.id
                else m.emisor.id
            end
            from Mensaje m
            where (m.emisor.id = :usuarioId or m.receptor.id = :usuarioId)
              and m.eliminado = false
            """)
    List<Long> findContactosId(@Param("usuarioId") Long usuarioId);
}
