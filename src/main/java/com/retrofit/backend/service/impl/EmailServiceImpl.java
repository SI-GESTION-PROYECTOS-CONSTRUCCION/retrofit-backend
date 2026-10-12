package com.retrofit.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrofit.backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.mail.from:}")
    private String configuredMailFrom;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetUrl) {
        String subject = "Recuperación de Contraseña - Retrofit";
        String displayName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Usuario";

        String htmlContent = buildResetPasswordTemplate(displayName, resetUrl);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO_API_KEY no está configurada. No se pudo enviar el correo a {}", toEmail);
            throw new RuntimeException("El servicio de correo no está configurado (falta BREVO_API_KEY).");
        }

        String fromAddress = (configuredMailFrom != null && !configuredMailFrom.isBlank())
                ? configuredMailFrom.trim()
                : "";

        if (fromAddress.isBlank()) {
            log.warn("APP_MAIL_FROM no está configurada en las variables de entorno.");
            throw new RuntimeException("El correo remitente no está configurado (falta APP_MAIL_FROM).");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", "Retrofit", "email", fromAddress),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Correo enviado exitosamente vía Brevo API a: {}", toEmail);
            } else {
                log.error("Fallo al enviar correo con Brevo API. HTTP {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Error en Brevo API (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            log.error("Error al enviar correo vía Brevo API a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error en servicio de correo: " + e.getMessage(), e);
        }
    }

    private String buildResetPasswordTemplate(String name, String resetUrl) {
        return "<!DOCTYPE html>"
                + "<html lang='es'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Restablecer Contraseña</title>"
                + "<style>"
                + "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #1e293b; }"
                + "  .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); }"
                + "  .header { background: #0f172a; padding: 32px 24px; text-align: center; }"
                + "  .header h1 { color: #ffffff; margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }"
                + "  .header span { color: #38bdf8; font-size: 13px; text-transform: uppercase; font-weight: 600; letter-spacing: 1.5px; display: block; margin-bottom: 4px; }"
                + "  .content { padding: 36px 32px; font-size: 15px; line-height: 1.6; color: #334155; }"
                + "  .btn-wrapper { text-align: center; margin: 32px 0; }"
                + "  .btn { display: inline-block; background-color: #0284c7; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: 600; font-size: 15px; transition: background-color 0.2s ease; }"
                + "  .notice { background: #f8fafc; border-left: 4px solid #0284c7; padding: 14px 16px; margin: 24px 0; font-size: 13.5px; color: #64748b; border-radius: 0 8px 8px 0; }"
                + "  .footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }"
                + "  .link-fallback { word-break: break-all; font-size: 12.5px; color: #0284c7; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "  <div class='header'>"
                + "    <span>SISTEMA DE GESTIÓN</span>"
                + "    <h1>RETROFIT</h1>"
                + "  </div>"
                + "  <div class='content'>"
                + "    <p>Hola <strong>" + name + "</strong>,</p>"
                + "    <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en <strong>Retrofit</strong>.</p>"
                + "    <p>Para continuar, haz clic en el siguiente botón:</p>"
                + "    <div class='btn-wrapper'>"
                + "      <a href='" + resetUrl + "' class='btn' target='_blank'>Restablecer Contraseña</a>"
                + "    </div>"
                + "    <div class='notice'>"
                + "      <strong>Importante:</strong> Este enlace expirará en <strong>15 minutos</strong> y solo puede ser utilizado una vez. Si no solicitaste este cambio, puedes ignorar este correo; tu contraseña actual permanecerá intacta."
                + "    </div>"
                + "    <p style='margin-top: 24px; font-size: 13px; color: #64748b;'>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>"
                + "    <p class='link-fallback'>" + resetUrl + "</p>"
                + "  </div>"
                + "  <div class='footer'>"
                + "    <p>&copy; " + java.time.Year.now().getValue() + " Retrofit. Todos los derechos reservados.</p>"
                + "    <p>Este es un correo automático, por favor no respondas a este mensaje.</p>"
                + "  </div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}
