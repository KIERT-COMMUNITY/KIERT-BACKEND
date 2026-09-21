package com.kiert.backend.repository;

import com.kiert.backend.entity.AuditoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {
}