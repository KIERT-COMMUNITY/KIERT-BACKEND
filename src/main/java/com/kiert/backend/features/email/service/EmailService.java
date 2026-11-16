// src/main/java/com/kiert/backend/features/email/service/EmailService.java
package com.kiert.backend.features.email.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate stringRedis;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${kiert.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private static final String KEY_RATELIMIT_DESTINO = "ratelimit:email:destino:";
    private static final String KEY_RATELIMIT_GLOBAL = "ratelimit:email:global";

    private static final int MAX_EMAILS_POR_DESTINO = 5;
    private static final Duration VENTANA_DESTINO = Duration.ofHours(1);

    private static final int MAX_EMAILS_GLOBAL = 500;
    private static final Duration VENTANA_GLOBAL = Duration.ofHours(1);

    private static final int MAX_NOMBRE_USUARIO_LENGTH = 100;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // ============================================================
    // CODIGO DE VERIFICACION DE CUENTA
    // ============================================================
    public void enviarCodigoVerificacion(String emailDestino, String nombreUsuario, String codigo) {
        log.info("[EMAIL] Intentando enviar correo de verificacion a: {}", emailDestino);

        String destinatario = normalizarEmail(emailDestino);
        if (!validarDestinatario(destinatario)) {
            log.warn("[EMAIL] Destinatario invalido: {}", emailDestino);
            return;
        }
        if (!puedeEnviar(destinatario, "verificacion")) {
            log.warn("[EMAIL] Rate limit alcanzado, no se envia verificacion a: {}", emailDestino);
            return;
        }

        try {
            String html = plantillaCodigo(
                    truncar(nombreUsuario, MAX_NOMBRE_USUARIO_LENGTH),
                    "Verifica tu cuenta",
                    "Usa el siguiente codigo para activar tu cuenta en Kiert:",
                    codigo
            );
            log.info("[EMAIL] Llamando a enviarHtml para: {}", emailDestino);
            enviarHtml(destinatario, "Kiert - Verifica tu cuenta", html);
            registrarEnvio(destinatario);
            log.info("[EMAIL] Codigo de verificacion enviado exitosamente a: {}", emailDestino);
        } catch (Exception e) {
            log.error("[EMAIL] ERROR al enviar verificacion a {}: {}", emailDestino, e.getMessage(), e);
        }
    }

    // ============================================================
    // CODIGO DE RECUPERACION DE CONTRASENA
    // ============================================================
    public void enviarCodigoRecuperacion(String emailDestino, String nombreUsuario, String codigo) {
        log.info("[EMAIL] Intentando enviar correo de recuperacion a: {}", emailDestino);

        String destinatario = normalizarEmail(emailDestino);
        if (!validarDestinatario(destinatario)) {
            log.warn("[EMAIL] Destinatario invalido: {}", emailDestino);
            return;
        }
        if (!puedeEnviar(destinatario, "recuperacion")) {
            log.warn("[EMAIL] Rate limit alcanzado, no se envia recuperacion a: {}", emailDestino);
            return;
        }

        try {
            String html = plantillaCodigo(
                    truncar(nombreUsuario, MAX_NOMBRE_USUARIO_LENGTH),
                    "Recupera tu contrasena",
                    "Usa el siguiente codigo para restablecer tu contrasena en Kiert:",
                    codigo
            );
            log.info("[EMAIL] Llamando a enviarHtml para: {}", emailDestino);
            enviarHtml(destinatario, "Kiert - Recupera tu contrasena", html);
            registrarEnvio(destinatario);
            log.info("[EMAIL] Codigo de recuperacion enviado exitosamente a: {}", emailDestino);
        } catch (Exception e) {
            log.error("[EMAIL] ERROR al enviar recuperacion a {}: {}", emailDestino, e.getMessage(), e);
        }
    }

    // ============================================================
    // BIENVENIDA
    // ============================================================
    public void enviarCorreoBienvenida(String emailDestino, String nombreUsuario) {
        log.info("[EMAIL] Intentando enviar correo de bienvenida a: {}", emailDestino);

        String destinatario = normalizarEmail(emailDestino);
        if (!validarDestinatario(destinatario)) {
            log.warn("[EMAIL] Destinatario invalido: {}", emailDestino);
            return;
        }
        if (!puedeEnviar(destinatario, "bienvenida")) {
            log.warn("[EMAIL] Rate limit alcanzado, no se envia bienvenida a: {}", emailDestino);
            return;
        }

        try {
            String html = plantillaBienvenida(truncar(nombreUsuario, MAX_NOMBRE_USUARIO_LENGTH));
            log.info("[EMAIL] Llamando a enviarHtml para: {}", emailDestino);
            enviarHtml(destinatario, "Bienvenido a Kiert", html);
            registrarEnvio(destinatario);
            log.info("[EMAIL] Correo de bienvenida enviado exitosamente a: {}", emailDestino);
        } catch (Exception e) {
            log.error("[EMAIL] ERROR al enviar bienvenida a {}: {}", emailDestino, e.getMessage(), e);
        }
    }

    // ============================================================
    // RATE LIMITING
    // ============================================================
    private boolean puedeEnviar(String emailDestino, String tipo) {
        try {
            String keyDestino = KEY_RATELIMIT_DESTINO + emailDestino;
            String valorDestino = stringRedis.opsForValue().get(keyDestino);
            if (valorDestino != null && Long.parseLong(valorDestino) >= MAX_EMAILS_POR_DESTINO) {
                log.warn("[EMAIL] Rate limit destino alcanzado para {} ({})", emailDestino, tipo);
                return false;
            }

            String valorGlobal = stringRedis.opsForValue().get(KEY_RATELIMIT_GLOBAL);
            if (valorGlobal != null && Long.parseLong(valorGlobal) >= MAX_EMAILS_GLOBAL) {
                log.error("[EMAIL] Rate limit GLOBAL alcanzado. Protegiendo SMTP.");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("[EMAIL] Error consultando rate limit en Redis: {}", e.getMessage());
            return false; // Fail-closed
        }
    }

    private void registrarEnvio(String emailDestino) {
        try {
            String keyDestino = KEY_RATELIMIT_DESTINO + emailDestino;
            Long nuevoDestino = stringRedis.opsForValue().increment(keyDestino);
            if (nuevoDestino != null && nuevoDestino == 1L) {
                stringRedis.expire(keyDestino, VENTANA_DESTINO);
            }

            Long nuevoGlobal = stringRedis.opsForValue().increment(KEY_RATELIMIT_GLOBAL);
            if (nuevoGlobal != null && nuevoGlobal == 1L) {
                stringRedis.expire(KEY_RATELIMIT_GLOBAL, VENTANA_GLOBAL);
            }
        } catch (Exception e) {
            log.error("[EMAIL] Error actualizando rate limit en Redis: {}", e.getMessage());
        }
    }

    // ============================================================
    // VALIDACIONES Y HELPERS
    // ============================================================
    private String normalizarEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean validarDestinatario(String emailDestino) {
        if (emailDestino == null || emailDestino.isBlank()) return false;
        if (!EMAIL_PATTERN.matcher(emailDestino).matches()) return false;
        if (emailDestino.contains("\n") || emailDestino.contains("\r")) return false;
        return true;
    }

    private void enviarHtml(String destinatario, String asunto, String html) throws Exception {
        if (asunto == null || asunto.contains("\n") || asunto.contains("\r")) {
            throw new IllegalArgumentException("Asunto invalido");
        }

        // Forzamos un timeout de 3000ms (3 segundos) para no colgar el hilo de Render
        Properties props = new Properties();
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setFrom(fromEmail, "Kiert");
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(html, true);

        log.info("[EMAIL] Enviando mensaje por SMTP...");
        mailSender.send(mensaje);
        log.info("[EMAIL] Mensaje SMTP enviado.");
    }

    private String truncar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    private String enmascararEmail(String email) {
        if (email == null || email.isBlank()) return "***";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String dominio = email.substring(at + 1);
        String localMask = local.length() <= 1 ? "***" : local.charAt(0) + "***";
        int dot = dominio.lastIndexOf('.');
        String dominioMask = dot <= 0 ? "***" : dominio.charAt(0) + "***" + dominio.substring(dot);
        return localMask + "@" + dominioMask;
    }

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
                            Kiert - Comunidad para compartir y ayudar
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
                .replace("__TITULO__", escapeHtml(titulo))
                .replace("__NOMBRE__", escapeHtml(nombreUsuario))
                .replace("__SUBTITULO__", escapeHtml(subtitulo))
                .replace("__CODIGO__", escapeHtml(codigo));
    }

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
                          <p style="margin:8px 0 0;color:#8b98a5;font-size:0.75rem;">Comunidad para compartir y ayudar</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:36px 32px;">
                          <h1 style="margin:0 0 12px;font-size:1.5rem;color:#e6edf3;font-weight:700;">
                            Bienvenido, __NOMBRE__
                          </h1>
                          <p style="margin:0 0 20px;color:#8b98a5;font-size:0.95rem;line-height:1.6;">
                            Tu cuenta ha sido activada correctamente. Ya eres parte de <strong style="color:#2dd4bf;">Kiert</strong>, la comunidad donde puedes compartir casos, experiencias, dudas y ayudar a otras personas.
                          </p>

                          <div style="background:#0d1117;border-radius:12px;padding:20px;margin:20px 0;">
                            <h2 style="margin:0 0 12px;font-size:0.95rem;color:#e6edf3;font-weight:600;">Que puedes hacer ahora?</h2>
                            <ul style="margin:0;padding-left:20px;color:#8b98a5;font-size:0.85rem;line-height:1.9;">
                              <li>Publicar tus casos y experiencias</li>
                              <li>Comentar y ayudar a otras personas</li>
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
                            Kiert - Comunidad para compartir y ayudar
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
                .replace("__NOMBRE__", escapeHtml(nombreUsuario))
                .replace("__URL_COMUNIDAD__", escapeHtml(urlComunidad))
                .replace("__FROM_EMAIL__", escapeHtml(fromEmail));
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}