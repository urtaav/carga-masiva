package com.tech.apicargamasiva.dto;

import com.tech.apicargamasiva.model.Sueldo;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SueldoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Número de empleado es requerido")
    @Size(max = 50, message = "Número de empleado muy largo")
    private String numeroEmpleado;

    @NotBlank(message = "Nombre completo es requerido")
    @Size(max = 200, message = "Nombre completo muy largo")
    private String nombreCompleto;

    @NotBlank(message = "Puesto es requerido")
    @Size(max = 100, message = "Puesto muy largo")
    private String puesto;

    @NotNull(message = "Salario base es requerido")
    @DecimalMin(value = "0.0", inclusive = false, message = "Salario base debe ser mayor que 0")
    private BigDecimal salarioBase;

    @NotNull(message = "Bonos no puede ser nulo")
    private BigDecimal bonos;

    @NotNull(message = "Deducciones no puede ser nulo")
    private BigDecimal deducciones;

    @NotNull(message = "Salario neto es requerido")
    private BigDecimal salarioNeto;

    @NotBlank(message = "Periodo de pago es requerido")
    @Size(max = 20, message = "Periodo de pago muy largo")
    private String periodoPago;

    @NotNull(message = "Fecha de pago es requerida")
    private LocalDate fechaPago;

    // Constructor desde entidad
    public SueldoDTO(Sueldo sueldo) {
        this.numeroEmpleado = sueldo.getNumeroEmpleado();
        this.nombreCompleto = sueldo.getNombreCompleto();
        this.puesto = sueldo.getPuesto();
        this.salarioBase = sueldo.getSalarioBase();
        this.bonos = sueldo.getBonos();
        this.deducciones = sueldo.getDeducciones();
        this.salarioNeto = sueldo.getSalarioNeto();
        this.periodoPago = sueldo.getPeriodoPago();
        this.fechaPago = sueldo.getFechaPago();
    }

    // Método para convertir a entidad
    public Sueldo toEntity() {
        return Sueldo.builder()
                .numeroEmpleado(this.numeroEmpleado)
                .nombreCompleto(this.nombreCompleto)
                .puesto(this.puesto)
                .salarioBase(this.salarioBase)
                .bonos(this.bonos)
                .deducciones(this.deducciones)
                .salarioNeto(this.salarioNeto)
                .periodoPago(this.periodoPago)
                .fechaPago(this.fechaPago)
                .build();
    }
}
