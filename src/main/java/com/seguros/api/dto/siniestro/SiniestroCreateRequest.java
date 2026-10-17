package com.seguros.api.dto.siniestro;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de déclaration d'un sinistre (POST /api/v1/siniestros).
 *
 * Protection Mass Assignment :
 * Ne contient JAMAIS :
 * - id : clé primaire autogénérée.
 * - numero : identifiant métier généré automatiquement par le serveur (ex: SIN-2026-0001).
 * - estado : fixé obligatoirement à ABIERTO à la déclaration.
 * - dates d'audit : gérées automatiquement par JPA.
 */
public record SiniestroCreateRequest(

    @NotNull(message = "El ID de la póliza es obligatorio")
    Long polizaId,

    @NotBlank(message = "La descripción del siniestro es obligatoria")
    @Size(min = 10, max = 1000, message = "La descripción debe tener entre 10 y 1000 caracteres")
    String descripcion,

    @NotNull(message = "La fecha de ocurrencia es obligatoria")
    @PastOrPresent(message = "La fecha de ocurrencia no puede ser futura")
    LocalDate fechaOcurrencia,

    @NotNull(message = "El importe reclamado es obligatorio")
    @Positive(message = "El importe reclamado debe ser estrictamente positivo")
    @Digits(integer = 10, fraction = 2, message = "El importe debe tener como máximo 10 dígitos enteros y 2 decimales")
    BigDecimal importeReclamado
) {
}
