package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.config.RabbitMQConfig;
import com.tech.apicargamasiva.dto.ChunkMessage;
import com.tech.apicargamasiva.dto.ImportacionResponse;
import com.tech.apicargamasiva.dto.JobStatusDTO;
import com.tech.apicargamasiva.dto.ProgressUpdate;
import com.tech.apicargamasiva.model.ImportacionJob;
import com.tech.apicargamasiva.repository.ImportacionJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class ImportacionService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ImportacionJobRepository jobRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${importacion.chunk-size:1000}")
    private int chunkSize;

    @Value("${importacion.temp-directory:./temp-uploads}")
    private String tempDirectory;

    public ImportacionResponse iniciarImportacion(MultipartFile file, String userEmail) throws IOException {

        // Generar Job ID
        String jobId = UUID.randomUUID().toString();

        // Guardar archivo temporalmente
        Path tempPath = guardarArchivo(file, jobId);

        // Validar formato
        if (!excelService.validarFormato(tempPath)) {
            throw new IllegalArgumentException("Formato de Excel inválido. Revise los headers.");
        }

        // Crear registro de Job
        ImportacionJob job = ImportacionJob.builder()
                .id(jobId)
                .filename(file.getOriginalFilename())
                .userEmail(userEmail)
                .status(ImportacionJob.JobStatus.VALIDANDO)
                .build();

        jobRepository.save(job);

        // Procesar asíncronamente
        procesarAsync(jobId, tempPath, userEmail);

        return ImportacionResponse.builder()
                .jobId(jobId)
                .message("Importación iniciada exitosamente")
                .statusUrl("/api/importacion/status/" + jobId)
                .build();
    }

    @Async("importacionExecutor")
    public void procesarAsync(String jobId, Path excelPath, String userEmail) {
        try {
            log.info("Iniciando procesamiento de job: {}", jobId);

            // Contar registros totales
            int totalRegistros = excelService.contarRegistros(excelPath);

            // Actualizar job
            actualizarJob(jobId, ImportacionJob.JobStatus.EN_PROCESO, totalRegistros, 0, 0, 0);

            // Dividir en chunks y enviar a RabbitMQ
            for (int i = 1; i <= totalRegistros; i += chunkSize) {
                ChunkMessage chunk = ChunkMessage.builder()
                        .jobId(jobId)
                        .filePath(excelPath.toString())
                        .startRow(i)
                        .endRow(Math.min(i + chunkSize - 1, totalRegistros))
                        .userEmail(userEmail)
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY,
                        chunk
                );

                log.debug("Chunk enviado: {} - {}", chunk.getStartRow(), chunk.getEndRow());
            }

        } catch (Exception e) {
            log.error("Error procesando importación {}: {}", jobId, e.getMessage(), e);
            actualizarJob(jobId, ImportacionJob.JobStatus.ERROR, 0, 0, 0, 0);
            emailService.enviarEmailError(userEmail, jobId, e.getMessage());
        }
    }

    private Path guardarArchivo(MultipartFile file, String jobId) throws IOException {
        Path uploadDir = Paths.get(tempDirectory);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String filename = jobId + "_" + file.getOriginalFilename();
        Path filePath = uploadDir.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath;
    }

    public void actualizarJob(String jobId, ImportacionJob.JobStatus status, Integer total,
                              Integer procesados, Integer exitosos, Integer errores) {

        ImportacionJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job no encontrado"));

        job.setStatus(status);
        if (total != null) job.setTotalRegistros(total);
        if (procesados != null) job.setProcesados(procesados);
        if (exitosos != null) job.setExitosos(exitosos);
        if (errores != null) job.setErrores(errores);

        jobRepository.save(job);

        // Actualizar en Redis
        redisTemplate.opsForValue().set("job:" + jobId, job, Duration.ofDays(7));

        // Notificar via WebSocket
        notificarProgreso(job);
    }

    private void notificarProgreso(ImportacionJob job) {
        double progreso = job.getTotalRegistros() > 0
                ? (job.getProcesados() * 100.0) / job.getTotalRegistros()
                : 0;

        ProgressUpdate update = ProgressUpdate.builder()
                .jobId(job.getId())
                .totalRegistros(job.getTotalRegistros())
                .procesados(job.getProcesados())
                .exitosos(job.getExitosos())
                .errores(job.getErrores())
                .progreso(progreso)
                .status(job.getStatus().toString())
                .build();

        messagingTemplate.convertAndSend("/topic/importacion/" + job.getId(), update);
    }

    public JobStatusDTO obtenerEstatus(String jobId) {
        // Intentar primero desde Redis
        ImportacionJob job = (ImportacionJob) redisTemplate.opsForValue().get("job:" + jobId);

        // Si no está en Redis, buscar en BD
        if (job == null) {
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job no encontrado"));
        }

        double progreso = job.getTotalRegistros() > 0
                ? (job.getProcesados() * 100.0) / job.getTotalRegistros()
                : 0;

        return JobStatusDTO.builder()
                .jobId(job.getId())
                .filename(job.getFilename())
                .status(job.getStatus())
                .totalRegistros(job.getTotalRegistros())
                .procesados(job.getProcesados())
                .exitosos(job.getExitosos())
                .errores(job.getErrores())
                .progreso(progreso)
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
