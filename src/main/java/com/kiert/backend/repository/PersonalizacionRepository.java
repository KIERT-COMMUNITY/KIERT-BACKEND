package com.kiert.backend.repository;

import com.kiert.backend.entity.PersonalizacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalizacionRepository extends JpaRepository<PersonalizacionUsuario, Long> {
    Optional<PersonalizacionUsuario> findByUsuarioId(Long usuarioId);
}