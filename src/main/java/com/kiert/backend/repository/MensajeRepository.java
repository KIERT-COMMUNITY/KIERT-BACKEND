package com.kiert.backend.repository;

import com.kiert.backend.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Todos los mensajes entre el usuario logueado y otro usuario, en orden cronológico
    @Query("""
            select m from Mensaje m
            where (m.emisor.id = :usuarioA and m.receptor.id = :usuarioB)
               or (m.emisor.id = :usuarioB and m.receptor.id = :usuarioA)
            order by m.fechaEnvio asc
            """)
    List<Mensaje> findConversacion(@Param("usuarioA") Long usuarioA, @Param("usuarioB") Long usuarioB);

    // Todos los mensajes donde el usuario participa (emisor o receptor), del más
    // reciente al más antiguo. El agrupado "último mensaje por interlocutor" se
    // hace en ChatService (más simple y portable que una GROUP BY con CASE en JPQL).
    @Query("""
            select m from Mensaje m
            where m.emisor.id = :usuarioId or m.receptor.id = :usuarioId
            order by m.fechaEnvio desc
            """)
    List<Mensaje> findTodosLosMensajesDeUsuario(@Param("usuarioId") Long usuarioId);

    long countByEmisorIdAndReceptorIdAndLeidoFalse(Long emisorId, Long receptorId);
}
