package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.dto.JobStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envía correo cuando una importación se completa exitosamente.
     */
    @Async("importacionExecutor")
    public void enviarEmailFinalizacion(String email, JobStatusDTO jobStatus) {
        try {
            String htmlContent = generarHtmlFinalizacion(jobStatus);
            enviarEmail(email, "✅ Importación Completada - " + jobStatus.getJobId(), htmlContent);
            log.info("Correo de finalización enviado a {}", email);
        } catch (Exception e) {
            log.error("Error enviando correo de finalización: {}", e.getMessage(), e);
        }
    }

    /**
     * Envía correo cuando ocurre un error en la importación.
     */
    @Async("importacionExecutor")
    public void enviarEmailError(String email, String jobId, String errorMessage) {
        try {
            String htmlContent = generarHtmlError(jobId, errorMessage);
            enviarEmail(email, "❌ Error en Importación - " + jobId, htmlContent);
            log.info("Correo de error enviado a {}", email);
        } catch (Exception e) {
            log.error("Error enviando correo de error: {}", e.getMessage(), e);
        }
    }

    /**
     * Método genérico para enviar correos HTML.
     */
    private void enviarEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom("noreply@empresa.com");

        mailSender.send(message);
    }

    /**
     * Genera el contenido HTML para un correo de finalización.
     */
    private String generarHtmlFinalizacion(JobStatusDTO jobStatus) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#2e7d32;">✅ Importación Completada</h2>
                    <p><b>ID del proceso:</b> %s</p>
                    <p><b>Archivo:</b> %s</p>
                    <p><b>Total de registros:</b> %d</p>
                    <p><b>Éxitosos:</b> %d</p>
                    <p><b>Errores:</b> %d</p>
                    <p><b>Estado:</b> %s</p>
                    <hr/>
                    <p style="font-size:12px; color:#777;">Este es un mensaje automático, no responda a este correo.</p>
                </body>
                </html>
                """.formatted(
                jobStatus.getJobId(),
                jobStatus.getFilename(),
                jobStatus.getTotalRegistros(),
                jobStatus.getExitosos(),
                jobStatus.getErrores(),
                jobStatus.getStatus().getDescripcion()
        );
    }

    /**
     * Genera el contenido HTML para un correo de error.
     */
    private String generarHtmlError(String jobId, String errorMessage) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2 style="color:#c62828;">❌ Error en Importación</h2>
                    <p><b>ID del proceso:</b> %s</p>
                    <p><b>Mensaje de error:</b></p>
                    <pre style="background-color:#f8d7da; padding:10px; border-radius:5px; color:#721c24;">%s</pre>
                    <hr/>
                    <p style="font-size:12px; color:#777;">Este es un mensaje automático, no responda a este correo.</p>
                </body>
                </html>
                """.formatted(jobId, errorMessage);
    }
}