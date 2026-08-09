package com.kiert.backend.service;

import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Soporta profile.component.ts: leer datos del usuario logueado y actualizar su foto
// una vez que el navegador terminó de subirla a Supabase (ver flujo en UploadService).
@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPerfil(Long usuarioId) {
        Usuario usuario = buscar(usuarioId);
        return aDTO(usuario);
    }

    @Transactional
    public UsuarioDTO actualizarFotoPerfil(Long usuarioId, String urlFoto) {
        Usuario usuario = buscar(usuarioId);
        usuario.setFotoPerfilUrl(urlFoto);
        usuario = usuarioRepository.save(usuario);
        return aDTO(usuario);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getNombreUsuario(), usuario.getEmail(), usuario.getFotoPerfilUrl());
    }
}
