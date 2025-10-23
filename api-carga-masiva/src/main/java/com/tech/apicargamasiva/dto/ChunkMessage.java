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
public class ChunkMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String filePath;
    private Integer startRow;
    private Integer endRow;
    private String userEmail;
    private Integer chunkNumber;
    private Integer totalChunks;

    @Override
    public String toString() {
        return String.format("ChunkMessage[jobId=%s, rows=%d-%d, chunk=%d/%d]",
                jobId, startRow, endRow, chunkNumber, totalChunks);
    }
}
