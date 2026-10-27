package com.kiert.backend.features.bienvenida.repository;

import com.kiert.backend.entity.ConfiguracionBienvenida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionBienvenidaRepository
        extends JpaRepository<ConfiguracionBienvenida, Long> {
}