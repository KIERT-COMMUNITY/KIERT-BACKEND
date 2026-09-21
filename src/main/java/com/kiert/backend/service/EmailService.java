package com.kiert.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${kiert.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // ============================================================
    // CODIGO DE VERIFICACION DE CUENTA
    // ============================================================
    @Async
    public void enviarCodigoVerificacion(String emailDestino, String nombreUsuario, String codigo) {
        try {
            String html = plantillaCodigo(
                    nombreUsuario,
                    "Verifica tu cuenta",
                    "Usa el siguiente codigo para activar tu cuenta en Kiert:",
                    codigo
            );
            enviarHtml(emailDestino, "Kiert - Verifica tu cuenta", html);
            log.info("Codigo de verificacion enviado a: {}", emailDestino);
        } catch (Exception e) {
            log.error("Error al enviar codigo de verificacion a {}: {}", emailDestino, e.getMessage(), e);
        }
    }

    // ============================================================
    // CODIGO DE RECUPERACION DE CONTRASENA
    // ============================================================
    @Async
    public void enviarCodigoRecuperacion(String emailDestino, String nombreUsuario, String codigo) {
        try {
            String html = plantillaCodigo(
                    nombreUsuario,
                    "Recupera tu contrasena",
                    "Usa el siguiente codigo para restablecer tu contrasena en Kiert:",
                    codigo
            );
            enviarHtml(emailDestino, "Kiert - Recupera tu contrasena", html);
            log.info("Codigo de recuperacion enviado a: {}", emailDestino);
        } catch (Exception e) {
            log.error("Error al enviar codigo de recuperacion a {}: {}", emailDestino, e.getMessage(), e);
        }
    }

    // ============================================================
    // BIENVENIDA (cuando se activa la cuenta)
    // ============================================================
    @Async
    public void enviarCorreoBienvenida(String emailDestino, String nombreUsuario) {
        try {
            String html = plantillaBienvenida(nombreUsuario);
            enviarHtml(emailDestino, "Bienvenido a Kiert, comunidad para desarrolladores", html);
            log.info("Correo de bienvenida enviado a: {}", emailDestino);
        } catch (Exception e) {
            log.error("Error al enviar bienvenida a {}: {}", emailDestino, e.getMessage(), e);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private void enviarHtml(String destinatario, String asunto, String html) throws Exception {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setFrom(fromEmail, "Kiert");
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(html, true);
        mailSender.send(mensaje);
    }

    // ============================================================
    // PLANTILLA: codigo de 6 digitos
    // ============================================================
    private String plantillaCodigo(String nombreUsuario, String titulo, String subtitulo, String codigo) {
        String plantilla = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#0d1117;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#0d1117;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:480px;background:#131a22;border:1px solid #26313c;border-radius:16px;overflow:hidden;">
                      <tr>
                        <td style="padding:32px 32px 24px;text-align:center;border-bottom:1px solid #26313c;">
                          <div style="font-family:'JetBrains Mono',monospace;font-size:1.4rem;font-weight:700;color:#e6edf3;">
                            <span style="color:#2dd4bf;">&gt;_</span> kiert
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px;">
                          <h1 style="margin:0 0 8px;font-size:1.3rem;color:#e6edf3;font-weight:700;">__TITULO__</h1>
                          <p style="margin:0 0 20px;color:#8b98a5;font-size:0.9rem;line-height:1.5;">
                            Hola <strong style="color:#2dd4bf;">__NOMBRE__</strong>, __SUBTITULO__
                          </p>

                          <div style="background:#0d1117;border:1px dashed #2dd4bf;border-radius:12px;padding:20px;text-align:center;margin:20px 0;">
                            <div style="font-family:'JetBrains Mono',monospace;font-size:2rem;font-weight:700;color:#2dd4bf;letter-spacing:8px;">
                              __CODIGO__
                            </div>
                          </div>

                          <p style="margin:16px 0 0;color:#8b98a5;font-size:0.8rem;line-height:1.5;">
                            El codigo expira en <strong style="color:#e6edf3;">15 minutos</strong>. Si no fuiste tu, ignora este correo.
                          </p>

                          <div style="margin-top:24px;padding:12px 16px;background:rgba(249,202,36,0.08);border-left:3px solid #f9ca24;border-radius:6px;">
                            <p style="margin:0;color:#f9ca24;font-size:0.78rem;line-height:1.5;">
                              <strong>No ves el correo?</strong><br>
                              Revisa tu carpeta de <strong>Spam</strong> o <strong>Promociones</strong> en Gmail y marcalo como "No es spam" para recibir futuros correos.
                            </p>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:20px 32px;text-align:center;border-top:1px solid #26313c;">
                          <p style="margin:0;color:#5a6a7a;font-size:0.7rem;">
                            Este es un mensaje automatico, no respondas a este correo.
                          </p>
                          <p style="margin:6px 0 0;color:#5a6a7a;font-size:0.7rem;">
                            Kiert - Comunidad para desarrolladores
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

        return plantilla
                .replace("__TITULO__", titulo)
                .replace("__NOMBRE__", nombreUsuario)
                .replace("__SUBTITULO__", subtitulo)
                .replace("__CODIGO__", codigo);
    }

    // ============================================================
    // PLANTILLA: bienvenida
    // ============================================================
    private String plantillaBienvenida(String nombreUsuario) {
        String plantilla = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#0d1117;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#0d1117;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#131a22;border:1px solid #26313c;border-radius:16px;overflow:hidden;">
                      <tr>
                        <td style="padding:32px;text-align:center;background:linear-gradient(135deg,#0d1117,#1b232c);border-bottom:1px solid #26313c;">
                          <div style="font-family:'JetBrains Mono',monospace;font-size:1.6rem;font-weight:700;color:#e6edf3;">
                            <span style="color:#2dd4bf;">&gt;_</span> kiert
                          </div>
                          <p style="margin:8px 0 0;color:#8b98a5;font-size:0.75rem;">Comunidad para desarrolladores</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:36px 32px;">
                          <h1 style="margin:0 0 12px;font-size:1.5rem;color:#e6edf3;font-weight:700;">
                            Bienvenido, __NOMBRE__
                          </h1>
                          <p style="margin:0 0 20px;color:#8b98a5;font-size:0.95rem;line-height:1.6;">
                            Tu cuenta ha sido activada correctamente. Ya eres parte de <strong style="color:#2dd4bf;">Kiert</strong>, la comunidad donde desarrolladores comparten casos, dudas e historias reales.
                          </p>

                          <div style="background:#0d1117;border-radius:12px;padding:20px;margin:20px 0;">
                            <h2 style="margin:0 0 12px;font-size:0.95rem;color:#e6edf3;font-weight:600;">Que puedes hacer ahora?</h2>
                            <ul style="margin:0;padding-left:20px;color:#8b98a5;font-size:0.85rem;line-height:1.9;">
                              <li>Publicar tus casos y proyectos</li>
                              <li>Comentar y ayudar a otros desarrolladores</li>
                              <li>Conectar con la comunidad</li>
                              <li>Personalizar tu perfil con temas y marcos</li>
                            </ul>
                          </div>

                          <div style="text-align:center;margin-top:24px;">
                            <a href="__URL_COMUNIDAD__"
                               style="display:inline-block;padding:12px 32px;background:#2dd4bf;color:#0d1117;text-decoration:none;border-radius:8px;font-weight:700;font-size:0.9rem;">
                              Explorar la comunidad
                            </a>
                          </div>

                          <div style="margin-top:24px;padding:12px 16px;background:rgba(249,202,36,0.08);border-left:3px solid #f9ca24;border-radius:6px;">
                            <p style="margin:0;color:#f9ca24;font-size:0.78rem;line-height:1.5;">
                              <strong>Consejo:</strong> anade <strong>__FROM_EMAIL__</strong> a tus contactos para que futuros correos no vayan a Spam.
                            </p>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:20px 32px;text-align:center;border-top:1px solid #26313c;">
                          <p style="margin:0;color:#5a6a7a;font-size:0.7rem;">
                            Kiert - Comunidad para desarrolladores
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

        String urlComunidad = frontendUrl + "/comunidad";

        return plantilla
                .replace("__NOMBRE__", nombreUsuario)
                .replace("__URL_COMUNIDAD__", urlComunidad)
                .replace("__FROM_EMAIL__", fromEmail);
    }
}