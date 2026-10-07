package com.kiert.backend.service;

import com.kiert.backend.dto.ArchivoSubidoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final CloudinaryService cloudinaryService;

    public String subirArchivo(MultipartFile archivo) {
        return cloudinaryService.subirArchivo(archivo);
    }

    public String subirArchivo(MultipartFile archivo, String carpeta) {
        return cloudinaryService.subirArchivo(archivo, carpeta);
    }

    public ArchivoSubidoDTO subirArchivoChat(
            MultipartFile archivo,
            String carpeta,
            String tipoArchivo
    ) {
        return cloudinaryService.subirArchivoChat(archivo, carpeta, tipoArchivo);
    }

    public void eliminarArchivo(String publicId, String resourceType) {
        cloudinaryService.eliminarArchivo(publicId, resourceType);
    }

    public String subirVideo(MultipartFile video, String carpeta) {
        Map<String, Object> resultado = cloudinaryService.subirVideo(video, carpeta);
        Object url = resultado.get("secure_url");
        return url == null ? null : url.toString();
    }

    public String subirGif(MultipartFile gif, String carpeta) {
        return cloudinaryService.subirGif(gif, carpeta);
    }

    public String subirImagen(MultipartFile imagen, String carpeta) {
        return cloudinaryService.subirImagen(imagen, carpeta);
    }

    public String subirMarco(MultipartFile archivo, String nombre) {
        return cloudinaryService.subirMarco(archivo, nombre);
    }

    public boolean esVideo(MultipartFile archivo) {
        return cloudinaryService.esVideo(archivo);
    }

    public boolean esAudio(MultipartFile archivo) {
        return cloudinaryService.esAudio(archivo);
    }

    public boolean esGif(MultipartFile archivo) {
        return cloudinaryService.esGif(archivo);
    }

    public boolean esImagen(MultipartFile archivo) {
        return cloudinaryService.esImagen(archivo);
    }

    public String getFormato(String contentType) {
        return cloudinaryService.getFormato(contentType);
    }

    public String generarUrlFirmadaSubida(String nombreArchivo) {
        return cloudinaryService.generarUrlFirmadaSubida(nombreArchivo);
    }

    public String urlPublica(String nombreArchivo) {
        return cloudinaryService.urlPublica(nombreArchivo);
    }

    public String sanitizar(String nombre) {
        return cloudinaryService.sanitizar(nombre);
    }
}
