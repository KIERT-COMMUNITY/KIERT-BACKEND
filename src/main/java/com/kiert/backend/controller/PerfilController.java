package com.kiert.backend.controller;

import com.kiert.backend.dto.ActualizarFotoPerfilDTO;
import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Soporta profile.component.ts: leer el perfil propio y actualizar la foto
// una vez que profile.component.ts terminó de subirla a Supabase.
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<UsuarioDTO> obtenerPerfil() {
        return ResponseEntity.ok(perfilService.obtenerPerfil(usuarioActual.id()));
    }

    @PatchMapping("/foto")
    public ResponseEntity<UsuarioDTO> actualizarFoto(@Valid @RequestBody ActualizarFotoPerfilDTO datos) {
        return ResponseEntity.ok(perfilService.actualizarFotoPerfil(usuarioActual.id(), datos.urlFoto()));
    }
}
