package com.kiert.backend.service;

import com.kiert.backend.dto.UrlFirmadaRequestDTO;
import com.kiert.backend.dto.UrlFirmadaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Espejo del backend que necesita upload.service.ts (frontend):
// entrega una URL firmada de Supabase para que el navegador suba el archivo directo
// (paso 1 del flujo descrito en el comentario de upload.service.ts).
@Service
@RequiredArgsConstructor
public class UploadService {

    private final StorageService storageService;

    public UrlFirmadaResponseDTO generarUrlFirmada(UrlFirmadaRequestDTO datos) {
        String urlSubida = storageService.generarUrlFirmadaSubida(datos.nombreArchivo());
        String urlPublica = storageService.urlPublica(datos.nombreArchivo());
        return new UrlFirmadaResponseDTO(urlSubida, urlPublica);
    }
}
