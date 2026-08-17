package com.kiert.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${kiert.frontend.reset-password-url}")
    private String resetPasswordUrl;

    public void enviarCorreoRecuperacion(String emailDestino, String token) {
        try {
            String resetLink = resetPasswordUrl + "?token=" + token;

            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(fromEmail);
            mensaje.setTo(emailDestino);
            mensaje.setSubject("🔐 Recuperación de contraseña - Kiert");

            String cuerpo = """
                    Hola,
                    
                    Has solicitado recuperar tu contraseña en Kiert.
                    
                    Haz clic en el siguiente enlace para restablecer tu contraseña:
                    %s
                    
                    Este enlace expirará en 1 hora.
                    
                    Si no solicitaste este cambio, ignora este correo.
                    
                    Saludos,
                    El equipo de Kiert
                    """.formatted(resetLink);

            mensaje.setText(cuerpo);

            mailSender.send(mensaje);
            log.info("✅ Correo de recuperación enviado a: {}", emailDestino);

        } catch (Exception e) {
            log.error("❌ Error al enviar correo a {}: {}", emailDestino, e.getMessage());
            throw new RuntimeException("Error al enviar el correo de recuperación");
        }
    }

    public void enviarCorreoBienvenida(String emailDestino, String nombreUsuario) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(fromEmail);
            mensaje.setTo(emailDestino);
            mensaje.setSubject("👋 Bienvenido a Kiert");

            String cuerpo = """
                    Hola %s,
                    
                    ¡Bienvenido a Kiert! 
                    
                    Somos una comunidad dedicada a la ciberseguridad donde puedes:
                    - Compartir casos de hacking
                    - Pedir ayuda a la comunidad
                    - Conectar con otros entusiastas de la seguridad
                    
                    ¡Comienza a explorar la comunidad!
                    
                    Saludos,
                    El equipo de Kiert
                    """.formatted(nombreUsuario);

            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            log.info("✅ Correo de bienvenida enviado a: {}", emailDestino);

        } catch (Exception e) {
            log.error("❌ Error al enviar correo de bienvenida a {}: {}", emailDestino, e.getMessage());
        }
    }
}