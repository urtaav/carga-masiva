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
public class ProgressUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private Integer totalRegistros;
    private Integer procesados;
    private Integer exitosos;
    private Integer errores;
    private Double progreso;
    private String status;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private Double velocidad; // registros por segundo
    private String tiempoRestante; // estimado
    private Double tasaExito; // porcentaje

    /**
     * Crea un ProgressUpdate desde un ImportacionJob
     */
    public static ProgressUpdate from(ImportacionJob job) {
        double prog = job.calcularProgreso();
        double velocidad = job.calcularVelocidad();
        String tiempoRestante = formatearDuracion(job.estimarTiempoRestante().getSeconds());
        double tasaExito = job.calcularTasaExito();

        String mensaje = generarMensaje(job);

        return ProgressUpdate.builder()
                .jobId(job.getId())
                .totalRegistros(job.getTotalRegistros())
                .procesados(job.getProcesados())
                .exitosos(job.getExitosos())
                .errores(job.getErrores())
                .progreso(prog)
                .status(job.getStatus().toString())
                .message(mensaje)
                .timestamp(LocalDateTime.now())
                .velocidad(velocidad)
                .tiempoRestante(tiempoRestante)
                .tasaExito(tasaExito)
                .build();
    }

    /**
     * Genera un mensaje descriptivo del progreso
     */
    private static String generarMensaje(ImportacionJob job) {
        switch (job.getStatus()) {
            case VALIDANDO:
                return "Validando archivo...";
            case EN_PROCESO:
                return String.format("Procesando: %d de %d registros (%.1f%%)",
                        job.getProcesados(), job.getTotalRegistros(), job.calcularProgreso());
            case COMPLETADO:
                return String.format("Completado: %d exitosos, %d errores",
                        job.getExitosos(), job.getErrores());
            case ERROR:
                return "Error en el procesamiento: " + job.getErrorMessage();
            case PAUSADO:
                return "Procesamiento pausado";
            case CANCELADO:
                return "Procesamiento cancelado";
            default:
                return "Estado desconocido";
        }
    }

    /**
     * Formatea una duración en segundos a formato legible
     */
    private static String formatearDuracion(long segundos) {
        if (segundos < 60) {
            return segundos + " segundos";
        } else if (segundos < 3600) {
            long minutos = segundos / 60;
            long segs = segundos % 60;
            return String.format("%d min %d seg", minutos, segs);
        } else {
            long horas = segundos / 3600;
            long minutos = (segundos % 3600) / 60;
            return String.format("%d h %d min", horas, minutos);
        }
    }

    /**
     * Verifica si el job está finalizado
     */
    public boolean isFinalizado() {
        return "COMPLETADO".equals(status) ||
               "ERROR".equals(status) ||
               "CANCELADO".equals(status);
    }

    /**
     * Verifica si hay errores
     */
    public boolean tieneErrores() {
        return errores != null && errores > 0;
    }

    /**
     * Obtiene el porcentaje de éxito formateado
     */
    public String getTasaExitoFormateada() {
        return String.format("%.1f%%", tasaExito);
    }

    /**
     * Obtiene la velocidad formateada
     */
    public String getVelocidadFormateada() {
        if (velocidad < 1) {
            return String.format("%.2f registros/seg", velocidad);
        }
        return String.format("%.0f registros/seg", velocidad);
    }
}
