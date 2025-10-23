package com.tech.apicargamasiva.repository;


import com.tech.apicargamasiva.model.ImportacionJob;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImportacionJobRepository extends JpaRepository<ImportacionJob, String> {

    /**
     * Busca jobs por email del usuario ordenados por fecha de creación
     */
    List<ImportacionJob> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    /**
     * Busca jobs por email con paginación
     */
    Page<ImportacionJob> findByUserEmail(String userEmail, Pageable pageable);

    /**
     * Busca jobs por status
     */
    List<ImportacionJob> findByStatus(ImportacionJob.JobStatus status);

    /**
     * Busca jobs por status con paginación
     */
    Page<ImportacionJob> findByStatus(ImportacionJob.JobStatus status, Pageable pageable);

    /**
     * Busca jobs por status y email
     */
    List<ImportacionJob> findByStatusAndUserEmail(ImportacionJob.JobStatus status, String userEmail);

    /**
     * Busca jobs creados después de una fecha
     */
    List<ImportacionJob> findByCreatedAtAfter(LocalDateTime fecha);

    /**
     * Busca jobs completados en un rango de fechas
     */
    @Query("SELECT j FROM ImportacionJob j WHERE j.completedAt BETWEEN :inicio AND :fin")
    List<ImportacionJob> findCompletadosEntreFechas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    /**
     * Cuenta jobs por status
     */
    Long countByStatus(ImportacionJob.JobStatus status);

    /**
     * Cuenta jobs por email
     */
    Long countByUserEmail(String userEmail);

    /**
     * Busca jobs en proceso que llevan más de X minutos
     */
    @Query("SELECT j FROM ImportacionJob j WHERE j.status = 'EN_PROCESO' " +
           "AND j.updatedAt < :limite")
    List<ImportacionJob> findJobsEstancados(@Param("limite") LocalDateTime limite);

    /**
     * Obtiene estadísticas generales
     */
    @Query("SELECT " +
           "COUNT(j) as total, " +
           "SUM(CASE WHEN j.status = 'COMPLETADO' THEN 1 ELSE 0 END) as completados, " +
           "SUM(CASE WHEN j.status = 'EN_PROCESO' THEN 1 ELSE 0 END) as enProceso, " +
           "SUM(CASE WHEN j.status = 'ERROR' THEN 1 ELSE 0 END) as conError, " +
           "SUM(j.totalRegistros) as totalRegistros, " +
           "SUM(j.exitosos) as totalExitosos, " +
           "SUM(j.errores) as totalErrores " +
           "FROM ImportacionJob j")
    Object[] obtenerEstadisticas();

    /**
     * Obtiene los últimos N jobs
     */
    List<ImportacionJob> findTop10ByOrderByCreatedAtDesc();

    /**
     * Busca jobs con errores
     */
    @Query("SELECT j FROM ImportacionJob j WHERE j.errores > 0 ORDER BY j.createdAt DESC")
    List<ImportacionJob> findJobsConErrores();

    /**
     * Actualiza el status de un job
     */
    @Modifying
    @Query("UPDATE ImportacionJob j SET j.status = :status, j.updatedAt = :updatedAt " +
           "WHERE j.id = :jobId")
    int actualizarStatus(
            @Param("jobId") String jobId,
            @Param("status") ImportacionJob.JobStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Incrementa los contadores de un job (thread-safe con version)
     */
    @Modifying
    @Query("UPDATE ImportacionJob j SET " +
           "j.procesados = j.procesados + :procesados, " +
           "j.exitosos = j.exitosos + :exitosos, " +
           "j.errores = j.errores + :errores, " +
           "j.updatedAt = :updatedAt " +
           "WHERE j.id = :jobId")
    int incrementarContadores(
            @Param("jobId") String jobId,
            @Param("procesados") int procesados,
            @Param("exitosos") int exitosos,
            @Param("errores") int errores,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Elimina jobs antiguos (limpieza)
     */
    @Modifying
    @Query("DELETE FROM ImportacionJob j WHERE j.createdAt < :fecha " +
           "AND j.status IN ('COMPLETADO', 'ERROR', 'CANCELADO')")
    int eliminarJobsAntiguos(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca jobs por filename
     */
    List<ImportacionJob> findByFilenameContainingIgnoreCase(String filename);

    /**
     * Verifica si existe un job activo para un usuario
     */
    @Query("SELECT COUNT(j) > 0 FROM ImportacionJob j " +
           "WHERE j.userEmail = :email " +
           "AND j.status IN ('VALIDANDO', 'EN_PROCESO')")
    boolean existeJobActivoParaUsuario(@Param("email") String email);

    /**
     * Obtiene el promedio de tiempo de procesamiento
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (j.completedAt - j.createdAt))) " +
           "FROM ImportacionJob j " +
           "WHERE j.status = 'COMPLETADO' AND j.completedAt IS NOT NULL")
    Double obtenerTiempoPromedioEnSegundos();
}
