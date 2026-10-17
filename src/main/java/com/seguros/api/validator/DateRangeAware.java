package com.seguros.api.validator;

import java.time.LocalDate;

/**
 * Interface marquant tout DTO contenant une période d'effet (date de début et date de fin).
 */
public interface DateRangeAware {
    LocalDate fechaInicio();
    LocalDate fechaFin();
}
