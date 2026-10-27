package com.kiert.backend.features.bienvenida.service;

import com.kiert.backend.entity.ConfiguracionBienvenida;
import com.kiert.backend.entity.HistorialBienvenida;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.bienvenida.dto.BienvenidaDTO;
import com.kiert.backend.features.bienvenida.dto.HistorialBienvenidaDTO;
import com.kiert.backend.features.bienvenida.repository.ConfiguracionBienvenidaRepository;
import com.kiert.backend.features.bienvenida.repository.HistorialBienvenidaRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BienvenidaService {

    private static final Long ID_UNICO = 1L;

    private final ConfiguracionBienvenidaRepository configRepo;
    private final HistorialBienvenidaRepository historialRepo;
    private final UsuarioRepository usuarioRepo;

    // ============================================================
    // OBTENER (lectura pública)
    // ============================================================
    @Transactional(readOnly = true)
    public BienvenidaDTO obtener() {
        ConfiguracionBienvenida config = configRepo.findById(ID_UNICO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de bienvenida no encontrada"));

        return new BienvenidaDTO(
                config.getTitulo(),
                config.getContenido(),
                config.getFechaActualizacion() != null
                        ? config.getFechaActualizacion().truncatedTo(ChronoUnit.MILLIS).toString()
                        : null,
                config.getActualizadoPor() != null ? config.getActualizadoPor().getId() : null,
                config.getActualizadoPor() != null ? config.getActualizadoPor().getNombreUsuario() : null
        );
    }

    // ============================================================
    // ACTUALIZAR (solo admin) + guardar historial
    // ============================================================
    @Transactional
    public BienvenidaDTO actualizar(Long adminId, String titulo, String contenido, String ip) {
        Usuario admin = usuarioRepo.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Admin no encontrado"));

        ConfiguracionBienvenida config = configRepo.findById(ID_UNICO)
                .orElseGet(() -> ConfiguracionBienvenida.builder()
                        .id(ID_UNICO)
                        .titulo("Bienvenido a Kiert Community")
                        .contenido("Mensaje de bienvenida por defecto.")
                        .activo(true)
                        .fechaCreacion(Instant.now())
                        .fechaActualizacion(Instant.now())
                        .build());

        // Guardar valores anteriores para el historial
        String tituloAnterior = config.getTitulo();
        String contenidoAnterior = config.getContenido();

        // Actualizar
        config.setTitulo(titulo.trim());
        config.setContenido(contenido.trim());
        config.setActualizadoPor(admin);
        config.setFechaActualizacion(Instant.now());
        configRepo.save(config);

        // Guardar historial
        HistorialBienvenida historial = HistorialBienvenida.builder()
                .tituloAnterior(tituloAnterior)
                .contenidoAnterior(contenidoAnterior)
                .tituloNuevo(config.getTitulo())
                .contenidoNuevo(config.getContenido())
                .cambiadoPor(admin)
                .fechaCambio(Instant.now())
                .ipCambio(ip)
                .build();
        historialRepo.save(historial);

        log.info("Bienvenida actualizada por {} (IP: {})", admin.getNombreUsuario(), ip);

        return new BienvenidaDTO(
                config.getTitulo(),
                config.getContenido(),
                config.getFechaActualizacion().truncatedTo(ChronoUnit.MILLIS).toString(),
                admin.getId(),
                admin.getNombreUsuario()
        );
    }

    // ============================================================
    // HISTORIAL (solo admin)
    // ============================================================
    @Transactional(readOnly = true)
    public List<HistorialBienvenidaDTO> listarHistorial(int page, int size) {
        Page<HistorialBienvenida> pageResult = historialRepo.findAllByOrderByFechaCambioDesc(
                PageRequest.of(page, size));

        return pageResult.getContent().stream()
                .map(h -> new HistorialBienvenidaDTO(
                        h.getId(),
                        h.getTituloAnterior(),
                        h.getContenidoAnterior(),
                        h.getTituloNuevo(),
                        h.getContenidoNuevo(),
                        h.getCambiadoPor() != null ? h.getCambiadoPor().getId() : null,
                        h.getCambiadoPor() != null ? h.getCambiadoPor().getNombreUsuario() : null,
                        h.getFechaCambio().truncatedTo(ChronoUnit.MILLIS).toString(),
                        h.getIpCambio()
                ))
                .toList();
    }
}