package com.kiert.backend.features.turnstile.service;


import com.kiert.backend.config.TurnstileProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);

    private final TurnstileProperties props;
    private final RestClient restClient;

    public TurnstileService(TurnstileProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Verifica el token de Cloudflare Turnstile.
     * @param token token que envía el frontend (cf-turnstile-response)
     * @return true si es válido
     */
    public boolean verify(String token) {
        if (!props.enabled()) {
            log.warn("Turnstile deshabilitado; se omite verificación");
            return true;
        }

        if (token == null || token.isBlank()) {
            log.warn("Turnstile: token vacío o nulo");
            return false;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", props.secretKey());
        form.add("response", token);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(props.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) {
                log.warn("Turnstile: respuesta nula");
                return false;
            }

            Boolean success = (Boolean) response.get("success");
            if (Boolean.TRUE.equals(success)) {
                return true;
            }

            @SuppressWarnings("unchecked")
            List<String> errorCodes = (List<String>) response.get("error-codes");
            log.warn("Turnstile falló. error-codes={}", errorCodes);
            return false;

        } catch (Exception e) {
            log.error("Error verificando Turnstile: {}", e.getMessage(), e);
            return false; // fail-closed
        }
    }

    public String getSiteKey() {
        return props.siteKey();
    }

    public boolean isEnabled() {
        return props.enabled();
    }
}