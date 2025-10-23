package com.tech.apicargamasiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tech.apicargamasiva.model.ImportacionJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String filename;
    private String userEmail;
    private ImportacionJob.JobStatus status;
    private Integer totalRegistros;
    private Integer procesados;
    private Integer exitosos;
    private Integer errores;
    private String errorMessage;
    private Double progreso;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    // Constructor desde entidad
    public JobStatusDTO(ImportacionJob job) {
        this.jobId = job.getId();
        this.filename = job.getFilename();
        this.userEmail = job.getUserEmail();
        this.totalRegistros = job.getTotalRegistros();
        this.procesados = job.getProcesados();
        this.exitosos = job.getExitosos();
        this.errores = job.getErrores();
        this.errorMessage = job.getErrorMessage();
        this.progreso = job.getTotalRegistros() > 0
                ? (job.getProcesados() * 100.0) / job.getTotalRegistros()
                : 0.0;
        this.createdAt = job.getCreatedAt();
        this.updatedAt = job.getUpdatedAt();
        this.completedAt = job.getCompletedAt();
    }
}
