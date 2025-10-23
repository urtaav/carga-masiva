package com.tech.apicargamasiva.consumer;

import com.tech.apicargamasiva.config.RabbitMQConfig;
import com.tech.apicargamasiva.dto.ChunkMessage;
import com.tech.apicargamasiva.dto.ImportacionErrorDTO;
import com.tech.apicargamasiva.dto.JobStatusDTO;
import com.tech.apicargamasiva.dto.SueldoDTO;
import com.tech.apicargamasiva.model.ImportacionJob;
import com.tech.apicargamasiva.repository.ImportacionErrorRepository;
import com.tech.apicargamasiva.repository.ImportacionJobRepository;
import com.tech.apicargamasiva.service.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class ImportacionConsumer {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private ValidacionService validacionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImportacionService importacionService;

    @Autowired
    private ImportacionJobRepository jobRepository;

    @Autowired
    private ImportacionErrorService importacionErrorService;

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE, concurrency = "5-10")
    @Retry(name = "importacionRetry", fallbackMethod = "procesarChunkFallback")
    @CircuitBreaker(name = "importacionCB", fallbackMethod = "procesarChunkFallback")
    @Bulkhead(name = "importacionBulkhead")
    public void procesarChunk(ChunkMessage chunk) {
        long startTime = System.currentTimeMillis();

        log.info("📦 Procesando chunk del job {}: filas {} a {}",
                chunk.getJobId(), chunk.getStartRow(), chunk.getEndRow());

        try {
            // 1. Leer chunk del Excel
            List<SueldoDTO> sueldos = excelService.leerChunk(
                    Paths.get(chunk.getFilePath()),
                    chunk.getStartRow(),
                    chunk.getEndRow()
            );

            if (sueldos.isEmpty()) {
                log.warn("⚠️ Chunk vacío para job {}", chunk.getJobId());
                return;
            }

            // 2. Validar y separar válidos de inválidos
            List<SueldoDTO> validos = new ArrayList<>();
            List<ImportacionErrorDTO> errores = new ArrayList<>();

            for (int i = 0; i < sueldos.size(); i++) {
                SueldoDTO sueldo = sueldos.get(i);
                int rowNumber = chunk.getStartRow() + i;

                try {
                    validacionService.validarSueldo(sueldo);
                    validos.add(sueldo);
                } catch (ValidationException e) {
                    ImportacionErrorDTO error = ImportacionErrorDTO.builder()
                            .jobId(chunk.getJobId())
                            .rowNumber(rowNumber)
                            .numeroEmpleado(sueldo.getNumeroEmpleado())
                            .errorMessage(e.getMessage())
                            .errorType("VALIDATION_ERROR")
                            .rawData(convertirAMap(sueldo))
                            .retryable(false)
                            .build();
                    errores.add(error);

                    log.debug("❌ Validación fallida en fila {}: {}", rowNumber, e.getMessage());
                } catch (Exception e) {
                    log.error("⚠️ Error inesperado validando fila {}: {}", rowNumber, e.getMessage());
                }
            }

            // 3. Bulk insert de registros válidos
            int insertados = 0;
            if (!validos.isEmpty()) {
                insertados = bulkInsert(validos);
                log.info("✅ Insertados {} de {} registros válidos del chunk",
                        insertados, validos.size());
            }

            // 4. Guardar errores en batch
            if (!errores.isEmpty()) {
                importacionErrorService.guardarErrores(errores);
                log.warn("⚠️ Guardados {} errores del chunk", errores.size());
            }

            // 5. Actualizar progreso del job
            actualizarProgresoJob(chunk.getJobId(), sueldos.size(), insertados, errores.size());

            // 6. Verificar si es el último chunk y notificar
            verificarFinalizacion(chunk);

            long duration = System.currentTimeMillis() - startTime;
            log.info("⏱️ Chunk procesado en {} ms ({} reg/seg)",
                    duration, (sueldos.size() * 1000.0) / duration);

        } catch (Exception e) {
            log.error("💥 Error crítico procesando chunk del job {}: {}",
                    chunk.getJobId(), e.getMessage(), e);
            throw new RuntimeException("Error procesando chunk", e);
        }
    }

    @Transactional
    public int bulkInsert(List<SueldoDTO> sueldos) {
        String sql = """
            INSERT INTO sueldos 
            (numero_empleado, nombre_completo, puesto, salario_base, bonos, 
             deducciones, salario_neto, periodo_pago, fecha_pago, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (numero_empleado, periodo_pago) 
            DO UPDATE SET 
                nombre_completo = EXCLUDED.nombre_completo,
                puesto = EXCLUDED.puesto,
                salario_base = EXCLUDED.salario_base,
                bonos = EXCLUDED.bonos,
                deducciones = EXCLUDED.deducciones,
                salario_neto = EXCLUDED.salario_neto,
                fecha_pago = EXCLUDED.fecha_pago,
                updated_at = EXCLUDED.updated_at
            """;

        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SueldoDTO sueldo = sueldos.get(i);
                LocalDateTime now = LocalDateTime.now();

                ps.setString(1, sueldo.getNumeroEmpleado());
                ps.setString(2, sueldo.getNombreCompleto());
                ps.setString(3, sueldo.getPuesto());
                ps.setBigDecimal(4, sueldo.getSalarioBase());
                ps.setBigDecimal(5, sueldo.getBonos());
                ps.setBigDecimal(6, sueldo.getDeducciones());
                ps.setBigDecimal(7, sueldo.getSalarioNeto());
                ps.setString(8, sueldo.getPeriodoPago());
                ps.setObject(9, sueldo.getFechaPago());
                ps.setObject(10, now);
                ps.setObject(11, now);
            }

            @Override
            public int getBatchSize() {
                return sueldos.size();
            }
        });

        return Arrays.stream(results).sum();
    }

    @Transactional
    protected synchronized void actualizarProgresoJob(String jobId, int procesados,
                                                      int exitosos, int errores) {
        try {
            ImportacionJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job no encontrado: " + jobId));

            job.incrementarProcesados(exitosos, errores);
            jobRepository.save(job);

            // Notificar progreso via WebSocket
            importacionService.actualizarJob(
                    jobId,
                    job.getStatus(),
                    job.getTotalRegistros(),
                    job.getProcesados(),
                    job.getExitosos(),
                    job.getErrores()
            );

            log.debug("📊 Progreso actualizado: {}/{} ({:.1f}%)",
                    job.getProcesados(),
                    job.getTotalRegistros(),
                    job.calcularProgreso());

        } catch (Exception e) {
            log.error("Error actualizando progreso del job {}: {}", jobId, e.getMessage());
        }
    }

    protected void verificarFinalizacion(ChunkMessage chunk) {
        try {
            ImportacionJob job = jobRepository.findById(chunk.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job no encontrado"));

            // Si ya se procesaron todos los registros
            if (job.getProcesados() >= job.getTotalRegistros()) {
                job.marcarComoCompletado();
                jobRepository.save(job);

                log.info("🎉 Job {} COMPLETADO! Total: {}, Exitosos: {}, Errores: {}, Duración: {}",
                        job.getId(),
                        job.getTotalRegistros(),
                        job.getExitosos(),
                        job.getErrores(),
                        job.calcularDuracion());

                // Enviar email de finalización
                emailService.enviarEmailFinalizacion(
                        chunk.getUserEmail(),
                        new JobStatusDTO(job)
                );
            }
        } catch (Exception e) {
            log.error("Error verificando finalización del job {}: {}",
                    chunk.getJobId(), e.getMessage());
        }
    }

    public void procesarChunkFallback(ChunkMessage chunk, Exception e) {
        log.error("🔥 FALLBACK: Chunk del job {} falló después de todos los reintentos: {}",
                chunk.getJobId(), e.getMessage(), e);

        try {
            ImportacionJob job = jobRepository.findById(chunk.getJobId())
                    .orElse(null);

            if (job != null && !job.isCompleto()) {
                String errorMsg = "Error procesando chunk (filas " + chunk.getStartRow() +
                                  "-" + chunk.getEndRow() + "): " + e.getMessage();
                job.marcarComoError(errorMsg);
                jobRepository.save(job);

                // Notificar al usuario
                emailService.enviarEmailError(
                        chunk.getUserEmail(),
                        chunk.getJobId(),
                        errorMsg
                );
            }
        } catch (Exception ex) {
            log.error("Error en fallback: {}", ex.getMessage());
        }
    }

    private Map<String, Object> convertirAMap(SueldoDTO sueldo) {
        Map<String, Object> map = new HashMap<>();
        map.put("numeroEmpleado", sueldo.getNumeroEmpleado());
        map.put("nombreCompleto", sueldo.getNombreCompleto());
        map.put("puesto", sueldo.getPuesto());
        map.put("salarioBase", sueldo.getSalarioBase());
        map.put("bonos", sueldo.getBonos());
        map.put("deducciones", sueldo.getDeducciones());
        map.put("salarioNeto", sueldo.getSalarioNeto());
        map.put("periodoPago", sueldo.getPeriodoPago());
        map.put("fechaPago", sueldo.getFechaPago());
        return map;
    }
}