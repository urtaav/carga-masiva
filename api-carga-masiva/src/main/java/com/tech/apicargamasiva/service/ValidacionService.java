package com.tech.apicargamasiva.service;

import com.tech.apicargamasiva.dto.SueldoDTO;
import jakarta.validation.*;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ValidacionService {
    private final Validator validator;

    public ValidacionService() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public void validarSueldo(SueldoDTO sueldo) {
        Set<ConstraintViolation<SueldoDTO>> violations = validator.validate(sueldo);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<SueldoDTO> violation : violations) {
                sb.append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(sb.toString());
        }

        // Validaciones de negocio
        if (sueldo.getSalarioBase().compareTo(sueldo.getSalarioNeto()) < 0) {
            throw new ValidationException("Salario neto no puede ser mayor que salario base + bonos - deducciones");
        }
    }
}
