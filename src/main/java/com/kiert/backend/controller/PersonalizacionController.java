package com.kiert.backend.controller;

import com.kiert.backend.dto.PersonalizacionDTO;
import com.kiert.backend.dto.MarcoDTO;
import com.kiert.backend.dto.FondoDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PersonalizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/personalizacion")
@RequiredArgsConstructor
public class PersonalizacionController {

    private final PersonalizacionService personalizacionService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<PersonalizacionDTO> obtenerPersonalizacion() {
        Long userId = usuarioActual.id();
        log.info("GET /api/personalizacion - Usuario: {}", userId);
        return ResponseEntity.ok(personalizacionService.obtenerPersonalizacion(userId));
    }

    @PutMapping
    public ResponseEntity<PersonalizacionDTO> guardarPersonalizacion(@RequestBody PersonalizacionDTO datos) {
        Long userId = usuarioActual.id();
        log.info("PUT /api/personalizacion - Usuario: {}", userId);
        return ResponseEntity.ok(personalizacionService.guardarPersonalizacion(userId, datos));
    }

    @PostMapping("/foto-perfil")
    public ResponseEntity<PersonalizacionDTO> subirFotoPerfil(@RequestParam("archivo") MultipartFile archivo) {
        log.info("POST /api/personalizacion/foto-perfil - Usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(personalizacionService.subirFotoPerfil(usuarioActual.id(), archivo));
    }

    @PostMapping("/foto-portada")
    public ResponseEntity<PersonalizacionDTO> subirFotoPortada(@RequestParam("archivo") MultipartFile archivo) {
        log.info("POST /api/personalizacion/foto-portada - Usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(personalizacionService.subirFotoPortada(usuarioActual.id(), archivo));
    }

    @GetMapping("/marcos")
    public ResponseEntity<List<MarcoDTO>> obtenerMarcos() {
        log.info("GET /api/personalizacion/marcos - Usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(personalizacionService.obtenerMarcos(usuarioActual.id()));
    }

    @GetMapping("/fondos")
    public ResponseEntity<List<FondoDTO>> obtenerFondos() {
        log.info("GET /api/personalizacion/fondos - Usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(personalizacionService.obtenerFondos(usuarioActual.id()));
    }

    // ❌ ELIMINADOS: /comprar/marco, /comprar/fondo
}