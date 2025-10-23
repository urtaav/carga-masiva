package com.tech.apicargamasiva.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO que representa un error ocurrido durante el proceso de importación de sueldos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionErrorDTO {

    private Long id;                     // Opcional si lo expones al frontend
    private String jobId;                // ID del job al que pertenece el error
    private Integer rowNumber;           // Número de fila en el archivo origen
    private String numeroEmpleado;       // Número de empleado relacionado con el error
    private String errorMessage;         // Descripción del error
    private String errorType;            // Ejemplo: VALIDATION_ERROR, PARSE_ERROR, DB_ERROR, etc.
    private Map<String, Object> rawData; // Datos originales del registro en formato JSON
    private Boolean retryable;           // Si el error permite reintentar o no
    private LocalDateTime createdAt;     // Fecha de creación del error
}
