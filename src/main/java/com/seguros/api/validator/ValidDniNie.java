package com.seguros.api.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de validation pour les identifiants espagnols DNI (Documento Nacional de Identidad)
 * et NIE (Número de Identidad de Extranjero).
 *
 * Applique l'algorithme officiel modulo 23 du Ministère de l'Intérieur espagnol.
 */
@Documented
@Constraint(validatedBy = DniNieValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDniNie {

    String message() default "El DNI/NIE proporcionado no es válido según el algoritmo oficial (módulo 23)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
