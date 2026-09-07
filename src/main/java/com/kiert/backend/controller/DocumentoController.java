// src/main/java/com/kiert/backend/controller/DocumentoController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.CrearDocumentoDTO;
import com.kiert.backend.dto.DocumentoDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final UsuarioActual usuarioActual;

    // ========== CATEGORÍAS ==========

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> obtenerCategorias() {
        log.info("📋 Obteniendo categorías de documentos");
        return ResponseEntity.ok(documentoService.obtenerCategorias());
    }

    // ========== LISTAR ==========

    @GetMapping
    public ResponseEntity<List<DocumentoDTO>> listarTodos() {
        log.info("📋 Listando todos los documentos");
        return ResponseEntity.ok(documentoService.listarTodos());
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<DocumentoDTO>> listarPorCategoria(@PathVariable String categoria) {
        log.info("📋 Listando documentos por categoría: {}", categoria);
        return ResponseEntity.ok(documentoService.listarPorCategoria(categoria));
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<DocumentoDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        log.info("📋 Listando documentos del usuario: {}", usuarioId);
        return ResponseEntity.ok(documentoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDTO> obtenerPorId(@PathVariable Long id) {
        log.info("🔍 Obteniendo documento: {}", id);
        return ResponseEntity.ok(documentoService.obtenerPorId(id));
    }

    // ========== BUSCAR ==========

    @GetMapping("/buscar")
    public ResponseEntity<List<DocumentoDTO>> buscar(@RequestParam(required = false) String query) {
        log.info("🔍 Buscando documentos: {}", query);
        return ResponseEntity.ok(documentoService.buscar(query));
    }

    @GetMapping("/buscar/{categoria}")
    public ResponseEntity<List<DocumentoDTO>> buscarPorCategoria(
            @PathVariable String categoria,
            @RequestParam(required = false) String query) {
        log.info("🔍 Buscando documentos en categoría {}: {}", categoria, query);
        return ResponseEntity.ok(documentoService.buscarPorCategoria(categoria, query));
    }

    // ========== CREAR ==========

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoDTO> crearDocumento(
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "categoriaPersonalizada", required = false) String categoriaPersonalizada,
            @RequestParam("archivo") MultipartFile archivo) {

        Long usuarioId = usuarioActual.id();
        log.info("📝 Creando documento para usuario: {}", usuarioId);

        CrearDocumentoDTO datos = new CrearDocumentoDTO(titulo, descripcion, categoria, categoriaPersonalizada);
        DocumentoDTO creado = documentoService.crear(usuarioId, datos, archivo);

        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // ========== ACTUALIZAR ==========

    @PutMapping("/{id}")
    public ResponseEntity<DocumentoDTO> actualizarDocumento(
            @PathVariable Long id,
            @RequestBody CrearDocumentoDTO datos) {

        Long usuarioId = usuarioActual.id();
        log.info("✏️ Actualizando documento: {}", id);

        return ResponseEntity.ok(documentoService.actualizar(id, usuarioId, datos));
    }

    // ========== ELIMINAR ==========

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        log.info("🗑️ Eliminando documento: {}", id);
        documentoService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    // ========== DESCARGAR ==========

    @PostMapping("/{id}/descargar")
    public ResponseEntity<Void> incrementarDescargas(@PathVariable Long id) {
        log.info("📥 Incrementando descargas del documento: {}", id);
        documentoService.incrementarDescargas(id);
        return ResponseEntity.ok().build();
    }
}