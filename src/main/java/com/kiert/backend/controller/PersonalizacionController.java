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
        PersonalizacionDTO result = personalizacionService.obtenerPersonalizacion(userId);
        log.info("Personalizacion obtenida: tema={}, marco={}, fondo={}",
                result.temaId(), result.marcoId(), result.fondoId());
        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<PersonalizacionDTO> guardarPersonalizacion(@RequestBody PersonalizacionDTO datos) {
        Long userId = usuarioActual.id();
        log.info("PUT /api/personalizacion - Usuario: {}, tema={}, marco={}, fondo={}",
                userId, datos.temaId(), datos.marcoId(), datos.fondoId());
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

    @PostMapping("/marco")
    public ResponseEntity<MarcoDTO> subirMarco(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "precio", defaultValue = "0") Double precio) {
        log.info("POST /api/personalizacion/marco - Usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(personalizacionService.subirMarco(usuarioActual.id(), archivo, nombre, precio));
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

    @PostMapping("/comprar/marco/{marcoId}")
    public ResponseEntity<Void> comprarMarco(@PathVariable String marcoId) {
        log.info("POST /api/personalizacion/comprar/marco/{} - Usuario: {}", marcoId, usuarioActual.id());
        personalizacionService.comprarMarco(usuarioActual.id(), marcoId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comprar/fondo/{fondoId}")
    public ResponseEntity<Void> comprarFondo(@PathVariable String fondoId) {
        log.info("POST /api/personalizacion/comprar/fondo/{} - Usuario: {}", fondoId, usuarioActual.id());
        personalizacionService.comprarFondo(usuarioActual.id(), fondoId);
        return ResponseEntity.ok().build();
    }
}