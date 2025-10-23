package com.tech.apicargamasiva.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "importacion_errores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobId;
    private Integer rowNumber;
    private String numeroEmpleado;
    private String errorMessage;
    private String errorType;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> rawData;

    private Boolean retryable;
    private LocalDateTime createdAt;
}