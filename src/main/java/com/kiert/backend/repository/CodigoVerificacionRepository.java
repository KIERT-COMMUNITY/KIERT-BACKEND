package com.kiert.backend.repository;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    Optional<CodigoVerificacion> findTopByEmailAndCodigoAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
            String email, String codigo, TipoCodigo tipo);

    @Modifying
    @Query("UPDATE CodigoVerificacion c SET c.usado = true WHERE c.email = :email AND c.tipo = :tipo AND c.usado = false")
    void invalidarCodigosAnteriores(@Param("email") String email, @Param("tipo") TipoCodigo tipo);
}