package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.dto.ImportacionErrorDTO;
import com.tech.apicargamasiva.model.ImportacionError;
import com.tech.apicargamasiva.repository.ImportacionErrorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportacionErrorService {

    @Autowired
    private ImportacionErrorRepository repository;

    public void guardarErrores(List<ImportacionErrorDTO> errores) {
        repository.saveAll(errores.);
    }

    public void guardarError(ImportacionError error) {
        repository.save(error);
    }

    public List<ImportacionError> obtenerErroresPorJob(String jobId) {
        return repository.findByJobId(jobId);
    }

    public long contarErroresPorJob(String jobId) {
        return repository.countByJobId(jobId);
    }

    public void eliminarErroresPorJob(String jobId) {
        repository.deleteByJobId(jobId);
    }
}
