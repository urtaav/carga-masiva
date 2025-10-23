package com.tech.apicargamasiva.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sueldos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"numero_empleado", "periodo_pago"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sueldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Número de empleado es requerido")
    @Size(max = 50, message = "Número de empleado muy largo")
    @Column(name = "numero_empleado", nullable = false, length = 50)
    private String numeroEmpleado;


    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "puesto", nullable = false, length = 100)
    private String puesto;

    @Column(name = "salario_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "bonos", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bonos = BigDecimal.ZERO;

    @Column(name = "deducciones", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deducciones = BigDecimal.ZERO;

    @Column(name = "salario_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioNeto;

    @Column(name = "periodo_pago", nullable = false, length = 20)
    private String periodoPago;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        // Calcular salario neto automáticamente si no está establecido
        if (salarioNeto == null) {
            calcularSalarioNeto();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Recalcular salario neto en actualizaciones
        calcularSalarioNeto();
    }

    /**
     * Calcula el salario neto: salarioBase + bonos - deducciones
     */
    public void calcularSalarioNeto() {
        if (salarioBase != null) {
            BigDecimal bonosVal = (bonos != null) ? bonos : BigDecimal.ZERO;
            BigDecimal deduccionesVal = (deducciones != null) ? deducciones : BigDecimal.ZERO;

            this.salarioNeto = salarioBase
                    .add(bonosVal)
                    .subtract(deduccionesVal);
        }
    }

    /**
     * Validación de negocio
     */
    public boolean isValid() {
        if (salarioBase == null || salarioBase.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (salarioNeto == null || salarioNeto.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        if (numeroEmpleado == null || numeroEmpleado.trim().isEmpty()) {
            return false;
        }

        if (periodoPago == null || !periodoPago.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            return false;
        }

        return true;
    }
}
