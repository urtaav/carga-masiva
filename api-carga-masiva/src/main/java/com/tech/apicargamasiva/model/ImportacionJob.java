package com.tech.apicargamasiva.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(
        name = "importacion_jobs",
        indexes = {
                @Index(name = "idx_jobs_status", columnList = "status"),
                @Index(name = "idx_jobs_email", columnList = "user_email"),
                @Index(name = "idx_jobs_created", columnList = "created_at"),
                @Index(name = "idx_jobs_completed", columnList = "completed_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {})
@EqualsAndHashCode(of = {"id"})
public class ImportacionJob implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "total_registros")
    @Builder.Default
    private Integer totalRegistros = 0;

    @Column(name = "procesados")
    @Builder.Default
    private Integer procesados = 0;

    @Column(name = "exitosos")
    @Builder.Default
    private Integer exitosos = 0;

    @Column(name = "errores")
    @Builder.Default
    private Integer errores = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "started_processing_at")
    private LocalDateTime startedProcessingAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Estados del Job de Importación
     */
    public enum JobStatus {
        VALIDANDO("Validando archivo"),
        EN_PROCESO("Procesando registros"),
        COMPLETADO("Completado exitosamente"),
        ERROR("Error en procesamiento"),
        CANCELADO("Cancelado por usuario"),
        PAUSADO("Pausado temporalmente");

        private final String descripcion;

        JobStatus(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public boolean isFinalizado() {
            return this == COMPLETADO || this == ERROR || this == CANCELADO;
        }

        public boolean isEnProgreso() {
            return this == VALIDANDO || this == EN_PROCESO || this == PAUSADO;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = JobStatus.VALIDANDO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Si el status cambió a COMPLETADO o ERROR, establecer completedAt
        if ((status == JobStatus.COMPLETADO || status == JobStatus.ERROR)
            && completedAt == null) {
            completedAt = LocalDateTime.now();
        }

        // Si cambió a EN_PROCESO, establecer startedProcessingAt
        if (status == JobStatus.EN_PROCESO && startedProcessingAt == null) {
            startedProcessingAt = LocalDateTime.now();
        }
    }

    /**
     * Incrementa el contador de registros procesados de forma thread-safe
     */
    public synchronized void incrementarProcesados(int exitosos, int errores) {
        this.procesados += (exitosos + errores);
        this.exitosos += exitosos;
        this.errores += errores;
    }

    /**
     * Calcula el progreso en porcentaje
     */
    public double calcularProgreso() {
        if (totalRegistros == null || totalRegistros == 0) {
            return 0.0;
        }
        return (procesados * 100.0) / totalRegistros;
    }

    /**
     * Calcula la duración total del procesamiento
     */
    public Duration calcularDuracion() {
        if (createdAt == null) {
            return Duration.ZERO;
        }

        LocalDateTime fin = (completedAt != null) ? completedAt : LocalDateTime.now();
        return Duration.between(createdAt, fin);
    }

    /**
     * Calcula la velocidad de procesamiento (registros por segundo)
     */
    public double calcularVelocidad() {
        if (startedProcessingAt == null || procesados == 0) {
            return 0.0;
        }

        LocalDateTime fin = (completedAt != null) ? completedAt : LocalDateTime.now();
        long segundos = Duration.between(startedProcessingAt, fin).getSeconds();

        if (segundos == 0) {
            return 0.0;
        }

        return procesados / (double) segundos;
    }

    /**
     * Estima el tiempo restante basado en la velocidad actual
     */
    public Duration estimarTiempoRestante() {
        if (totalRegistros == null || procesados == 0 || totalRegistros <= procesados) {
            return Duration.ZERO;
        }

        double velocidad = calcularVelocidad();
        if (velocidad == 0) {
            return Duration.ZERO;
        }

        int registrosRestantes = totalRegistros - procesados;
        long segundosRestantes = (long) (registrosRestantes / velocidad);

        return Duration.ofSeconds(segundosRestantes);
    }

    /**
     * Calcula la tasa de éxito en porcentaje
     */
    public double calcularTasaExito() {
        if (procesados == null || procesados == 0) {
            return 0.0;
        }
        return (exitosos * 100.0) / procesados;
    }

    /**
     * Verifica si el job está completo
     */
    public boolean isCompleto() {
        return status == JobStatus.COMPLETADO || status == JobStatus.ERROR;
    }

    /**
     * Verifica si el job puede ser procesado
     */
    public boolean isProcesable() {
        return status == JobStatus.VALIDANDO || status == JobStatus.EN_PROCESO;
    }

    /**
     * Marca el job como error
     */
    public void marcarComoError(String mensaje) {
        this.status = JobStatus.ERROR;
        this.errorMessage = mensaje;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Marca el job como completado
     */
    public void marcarComoCompletado() {
        this.status = JobStatus.COMPLETADO;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Obtiene un resumen del job
     */
    public String obtenerResumen() {
        return String.format(
                "Job[%s]: %s - %d/%d procesados (%.1f%%) - %d exitosos, %d errores - %s",
                id.substring(0, 8),
                status,
                procesados,
                totalRegistros,
                calcularProgreso(),
                exitosos,
                errores,
                filename
        );
    }
}