package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizacionService {

    private final PersonalizacionRepository personalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    // ⚠️ QUITAMOS readOnly PARA PERMITIR INSERT SI NO EXISTE
    @Transactional
    public PersonalizacionDTO obtenerPersonalizacion(Long usuarioId) {
        log.info("Obteniendo personalizacion para usuario: {}", usuarioId);

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    log.info("No existe personalizacion, creando default para usuario: {}", usuarioId);
                    return crearPersonalizacionDefault(usuarioId);
                });

        return toDTO(personalizacion);
    }

    @Transactional
    public PersonalizacionDTO guardarPersonalizacion(Long usuarioId, PersonalizacionDTO datos) {
        log.info("Guardando personalizacion para usuario: {}", usuarioId);

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        if (datos.temaId() != null) personalizacion.setTemaId(datos.temaId());
        if (datos.marcoId() != null) personalizacion.setMarcoId(datos.marcoId());
        if (datos.fondoId() != null) personalizacion.setFondoId(datos.fondoId());

        if (datos.fotoPerfilUrl() != null) {
            personalizacion.setFotoPerfilUrl(datos.fotoPerfilUrl());
        }
        if (datos.fotoPortadaUrl() != null) {
            personalizacion.setFotoPortadaUrl(datos.fotoPortadaUrl());
        }

        personalizacion = personalizacionRepository.save(personalizacion);
        return toDTO(personalizacion);
    }

    @Transactional
    public PersonalizacionDTO subirFotoPerfil(Long usuarioId, MultipartFile archivo) {
        log.info("Subiendo foto de perfil para usuario: {}", usuarioId);

        String url = cloudinaryService.subirArchivo(archivo, "perfiles");

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        personalizacion.setFotoPerfilUrl(url);
        personalizacion = personalizacionRepository.save(personalizacion);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setFotoPerfilUrl(url);
        usuarioRepository.save(usuario);

        return toDTO(personalizacion);
    }

    @Transactional
    public PersonalizacionDTO subirFotoPortada(Long usuarioId, MultipartFile archivo) {
        log.info("Subiendo foto de portada para usuario: {}", usuarioId);

        String url = cloudinaryService.subirArchivo(archivo, "portadas");

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        personalizacion.setFotoPortadaUrl(url);
        personalizacion = personalizacionRepository.save(personalizacion);

        return toDTO(personalizacion);
    }

    @Transactional
    public MarcoDTO subirMarco(Long usuarioId, MultipartFile archivo, String nombre, Double precio) {
        log.info("Subiendo marco personalizado: {} para usuario: {}", nombre, usuarioId);

        String url = cloudinaryService.subirMarco(archivo, nombre);

        return new MarcoDTO(
                "custom_" + System.currentTimeMillis(),
                nombre,
                url,
                "circulo",
                precio != null ? precio : 0.0,
                precio == null || precio == 0
        );
    }

    @Transactional(readOnly = true)
    public List<MarcoDTO> obtenerMarcos(Long usuarioId) {
        log.info("Obteniendo marcos para usuario: {}", usuarioId);

        List<MarcoDTO> marcos = new ArrayList<>();

        marcos.add(new MarcoDTO("none", "Sin marco", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("classic", "Clasico", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("gold", "Dorado", null, "circulo", 2.0, false));
        marcos.add(new MarcoDTO("silver", "Plateado", null, "circulo", 2.0, false));
        marcos.add(new MarcoDTO("rainbow", "Arcoiris", null, "circulo", 3.0, false));
        marcos.add(new MarcoDTO("neon", "Neon", null, "circulo", 3.0, false));
        marcos.add(new MarcoDTO("square", "Cuadrado", null, "cuadrado", 2.0, false));
        marcos.add(new MarcoDTO("hexagon", "Hexagonal", null, "hexagonal", 4.0, false));

        return marcos;
    }

    @Transactional(readOnly = true)
    public List<FondoDTO> obtenerFondos(Long usuarioId) {
        log.info("Obteniendo fondos para usuario: {}", usuarioId);

        List<FondoDTO> fondos = new ArrayList<>();

        fondos.add(new FondoDTO("default", "Default", null, "gradiente", "linear-gradient(135deg, #0d1117, #161b22)", 0.0, true));
        fondos.add(new FondoDTO("dark", "Oscuro", null, "gradiente", "linear-gradient(135deg, #1a1a2e, #0d1117)", 0.0, true));
        fondos.add(new FondoDTO("light", "Claro", null, "gradiente", "linear-gradient(135deg, #ffffff, #f0f0f0)", 0.0, true));
        fondos.add(new FondoDTO("sunset", "Atardecer", null, "gradiente", "linear-gradient(135deg, #ff6b6b, #feca57, #ff9ff3)", 2.0, false));
        fondos.add(new FondoDTO("ocean", "Oceano", null, "gradiente", "linear-gradient(135deg, #00b894, #00cec9, #0984e3)", 2.0, false));
        fondos.add(new FondoDTO("aurora", "Aurora", null, "gradiente", "linear-gradient(135deg, #6c5ce7, #00b894, #fdcb6e)", 2.0, false));
        fondos.add(new FondoDTO("galaxy", "Galaxia", null, "gradiente", "linear-gradient(135deg, #2d3436, #6c5ce7, #fd79a8)", 3.0, false));

        return fondos;
    }

    @Transactional
    public void comprarMarco(Long usuarioId, String marcoId) {
        log.info("Comprando marco {} para usuario {}", marcoId, usuarioId);
    }

    @Transactional
    public void comprarFondo(Long usuarioId, String fondoId) {
        log.info("Comprando fondo {} para usuario {}", fondoId, usuarioId);
    }

    private PersonalizacionUsuario crearPersonalizacionDefault(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + usuarioId));

        PersonalizacionUsuario personalizacion = PersonalizacionUsuario.builder()
                .usuario(usuario)
                .temaId("default")
                .marcoId("none")
                .fondoId("default")
                .fotoPerfilUrl(usuario.getFotoPerfilUrl())
                .build();

        return personalizacionRepository.save(personalizacion);
    }

    private PersonalizacionDTO toDTO(PersonalizacionUsuario entity) {
        return new PersonalizacionDTO(
                entity.getId(),
                entity.getUsuario().getId(),
                entity.getTemaId(),
                entity.getMarcoId(),
                entity.getFondoId(),
                entity.getFotoPerfilUrl(),
                entity.getFotoPortadaUrl(),
                entity.getMarcoPersonalizadoUrl()
        );
    }
}