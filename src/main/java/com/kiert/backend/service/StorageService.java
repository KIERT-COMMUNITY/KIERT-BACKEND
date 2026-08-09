package com.kiert.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

// Encapsula la comunicación con Supabase Storage (bucket "kiert-files"),
// tal como describe el comentario de upload.service.ts en el frontend:
// 1) el backend valida y sube/firma, 2) el archivo queda accesible por URL pública.
@Service
@RequiredArgsConstructor
public class StorageService {

    private final WebClient.Builder webClientBuilder;
    private final SupabaseProperties supabaseProperties;

    // Usado por PostService cuando el usuario adjunta archivos directo al crear un post
    // (create-post.component.ts envía los archivos en el mismo FormData del post).
    public String subirArchivo(MultipartFile archivo) {
        String nombreUnico = UUID.randomUUID() + "-" + sanitizar(archivo.getOriginalFilename());
        String rutaDestino = "/storage/v1/object/" + supabaseProperties.bucket() + "/" + nombreUnico;

        try {
            webClientBuilder.build()
                    .post()
                    .uri(supabaseProperties.url() + rutaDestino)
                    .header("Authorization", "Bearer " + supabaseProperties.serviceRoleKey())
                    .header("Content-Type", archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream")
                    .bodyValue(archivo.getBytes())
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(30));
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo subir el archivo a Supabase Storage: " + ex.getMessage(), ex);
        }

        return urlPublica(nombreUnico);
    }

    // Usado por UploadService/profile.component.ts: el navegador pide una URL firmada
    // y sube el archivo DIRECTO a Supabase (sin pasar el peso por el backend).
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        String nombreUnico = UUID.randomUUID() + "-" + sanitizar(nombreArchivo);
        String rutaFirma = "/storage/v1/object/upload/sign/" + supabaseProperties.bucket() + "/" + nombreUnico;

        // Supabase responde { "url": "/object/upload/sign/...", "signedURL"?: ... } según la
        // versión de la API; tomamos el campo "url" y lo componemos con el host del proyecto.
        RespuestaFirma respuesta = webClientBuilder.build()
                .post()
                .uri(supabaseProperties.url() + rutaFirma)
                .header("Authorization", "Bearer " + supabaseProperties.serviceRoleKey())
                .header("Content-Type", "application/json")
                .bodyValue("{\"expiresIn\": 120}")
                .retrieve()
                .bodyToMono(RespuestaFirma.class)
                .block(Duration.ofSeconds(15));

        if (respuesta == null || respuesta.url() == null) {
            throw new RuntimeException("Supabase no devolvió una URL firmada válida.");
        }

        return supabaseProperties.url() + "/storage/v1" + respuesta.url();
    }

    private record RespuestaFirma(String url) {}

    public String urlPublica(String nombreArchivo) {
        return supabaseProperties.url() + "/storage/v1/object/public/" + supabaseProperties.bucket() + "/" + nombreArchivo;
    }

    private String sanitizar(String nombre) {
        return nombre == null ? "archivo" : nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
