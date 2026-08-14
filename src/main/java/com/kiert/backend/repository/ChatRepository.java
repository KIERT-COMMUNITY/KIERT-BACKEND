package com.kiert.backend.repository;

import com.kiert.backend.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Mensaje, Long> {

    @Query("""
            select m from Mensaje m
            where (m.emisor.id = :usuarioA and m.receptor.id = :usuarioB)
               or (m.emisor.id = :usuarioB and m.receptor.id = :usuarioA)
            order by m.fechaEnvio asc
            """)
    List<Mensaje> findConversacion(@Param("usuarioA") Long usuarioA, @Param("usuarioB") Long usuarioB);

    @Query("""
            select m from Mensaje m
            where m.emisor.id = :usuarioId or m.receptor.id = :usuarioId
            order by m.fechaEnvio desc
            """)
    List<Mensaje> findTodosLosMensajesDeUsuario(@Param("usuarioId") Long usuarioId);

    long countByEmisorIdAndReceptorIdAndLeidoFalse(Long emisorId, Long receptorId);

    @Query("""
            select distinct 
            case when m.emisor.id = :usuarioId then m.receptor.id else m.emisor.id end as contactoId
            from Mensaje m
            where m.emisor.id = :usuarioId or m.receptor.id = :usuarioId
            """)
    List<Long> findContactosId(@Param("usuarioId") Long usuarioId);
}