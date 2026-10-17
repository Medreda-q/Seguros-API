package com.seguros.api.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validateur vérifiant qu'un objet DateRangeAware a sa date de fin postérieure à sa date de début.
 * Si l'une des deux dates est nulle, la validation passe (délégué aux annotations @NotNull).
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRangeAware> {

    @Override
    public boolean isValid(DateRangeAware value, ConstraintValidatorContext context) {
        if (value == null || value.fechaInicio() == null || value.fechaFin() == null) {
            return true;
        }
        return value.fechaFin().isAfter(value.fechaInicio());
    }
}
