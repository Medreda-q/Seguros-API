package com.seguros.api.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de validation pour l'IBAN bancaire espagnol.
 * Le format espagnol officiel est composé de l'indicatif pays "ES" suivi de 22 chiffres (24 caractères au total).
 */
@Documented
@Constraint(validatedBy = IbanValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIban {

    String message() default "El IBAN proporcionado no es un IBAN español válido (debe comenzar por 'ES' seguido de 22 dígitos numéricos)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
