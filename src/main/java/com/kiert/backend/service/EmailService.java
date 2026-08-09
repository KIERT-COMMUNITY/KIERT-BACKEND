package com.kiert.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envía el correo de "recuperar contraseña" con el link que abre
// reset-password.component.ts (?token=...).
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${kiert.frontend.reset-password-url}")
    private String resetPasswordUrl;

    public void enviarCorreoRecuperacion(String destinatario, String token) {
        String enlace = resetPasswordUrl + "?token=" + token;

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("Recupera tu contraseña en Kiert");
            mensaje.setText("Recibimos una solicitud para restablecer tu contraseña.\n\n"
                    + "Haz clic en el siguiente enlace (válido por 2 horas):\n" + enlace
                    + "\n\nSi no fuiste tú, ignora este correo.");
            mailSender.send(mensaje);
        } catch (Exception ex) {
            // No queremos romper el flujo de "olvidé mi contraseña" si el SMTP falla en dev;
            // dejamos el link en el log para poder probar el flujo igual.
            log.warn("No se pudo enviar el correo de recuperación, enlace: {}", enlace, ex);
        }
    }
}
