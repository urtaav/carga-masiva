package com.tech.apicargamasiva.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportacionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String message;
    private String statusUrl;
    private String filename;
    private Integer estimatedRecords;
}
