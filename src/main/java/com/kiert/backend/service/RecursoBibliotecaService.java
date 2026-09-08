// src/main/java/com/kiert/backend/service/RecursoBibliotecaService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.RecursoBibliotecaDTO;
import com.kiert.backend.entity.RecursoBiblioteca;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.RecursoBibliotecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecursoBibliotecaService {

    private final RecursoBibliotecaRepository recursoRepository;

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> listarTodos() {
        log.info("📋 Listando todos los recursos de la biblioteca");
        return recursoRepository.findAllActiveOrderByFechaAgregadoDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> listarPorCategoria(String categoria) {
        log.info("📋 Listando recursos por categoría: {}", categoria);
        return recursoRepository.findByCategoriaOrderByFechaAgregadoDesc(categoria)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> listarDestacados() {
        log.info("⭐ Listando recursos destacados");
        return recursoRepository.findDestacados()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> obtenerCategorias() {
        log.info("📋 Obteniendo categorías de la biblioteca");
        return recursoRepository.findDistinctCategorias();
    }

    @Transactional(readOnly = true)
    public List<String> obtenerNiveles() {
        log.info("📋 Obteniendo niveles de la biblioteca");
        return recursoRepository.findDistinctNiveles();
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> buscar(String query) {
        log.info("🔍 Buscando recursos: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return listarTodos();
        }
        return recursoRepository.buscar(query.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> buscarPorCategoria(String categoria, String query) {
        log.info("🔍 Buscando recursos en categoría {}: {}", categoria, query);
        if (query == null || query.trim().isEmpty()) {
            return listarPorCategoria(categoria);
        }
        return recursoRepository.buscarPorCategoria(categoria, query.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecursoBibliotecaDTO obtenerPorId(Long id) {
        log.info("🔍 Obteniendo recurso: {}", id);
        RecursoBiblioteca recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));
        return toDTO(recurso);
    }

    // ========== DTO MAPPING ==========

    private RecursoBibliotecaDTO toDTO(RecursoBiblioteca entity) {
        List<String> tags = null;
        if (entity.getTags() != null && !entity.getTags().isEmpty()) {
            tags = Arrays.asList(entity.getTags().split(","));
        }

        return new RecursoBibliotecaDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getDescripcion(),
                entity.getUrl(),
                entity.getCategoria(),
                entity.getSubcategoria(),
                entity.getImagen(),
                entity.getAutor(),
                entity.getPlataforma(),
                entity.getDuracion(),
                entity.getNivel(),
                entity.getDestacado(),
                entity.getFechaAgregado(),
                tags
        );
    }
}