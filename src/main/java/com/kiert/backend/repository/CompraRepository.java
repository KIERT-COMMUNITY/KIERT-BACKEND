package com.kiert.backend.repository;

import com.kiert.backend.entity.CompraUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<CompraUsuario, Long> {
    List<CompraUsuario> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndTipoAndItemId(Long usuarioId, String tipo, String itemId);
}