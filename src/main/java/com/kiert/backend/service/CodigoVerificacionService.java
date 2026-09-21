package com.kiert.backend.service;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.repository.CodigoVerificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoVerificacionService {

    private final CodigoVerificacionRepository codigoRepository;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public String generarCodigo(String email, TipoCodigo tipo) {
        codigoRepository.invalidarCodigosAnteriores(email, tipo);

        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion entity = CodigoVerificacion.builder()
                .email(email)
                .codigo(codigo)
                .tipo(tipo)
                .build();

        codigoRepository.save(entity);
        log.info("Codigo generado para {} ({}): {}", email, tipo, codigo);
        return codigo;
    }

    @Transactional
    public void validarYConsumir(String email, String codigo, TipoCodigo tipo) {
        CodigoVerificacion entity = codigoRepository
                .findTopByEmailAndCodigoAndTipoAndUsadoFalseOrderByFechaCreacionDesc(email, codigo, tipo)
                .orElseThrow(() -> new BadRequestException("Codigo incorrecto o expirado"));

        if (!entity.estaVigente()) {
            throw new BadRequestException("El codigo ha expirado. Solicita uno nuevo.");
        }

        entity.setUsado(true);
        codigoRepository.save(entity);
    }
}