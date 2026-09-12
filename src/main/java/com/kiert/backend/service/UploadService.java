package com.kiert.backend.service;

import com.kiert.backend.dto.request.UrlFirmadaRequestDTO;
import com.kiert.backend.dto.response.UrlFirmadaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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