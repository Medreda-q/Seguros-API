package com.seguros.api.dto.cliente;

import com.seguros.api.validator.ValidIban;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de mise à jour des données de contact et bancaires du client (PUT /api/v1/clientes/{id}).
 *
 * Protection Mass Assignment & Intégrité du Domaine :
 * - Ne contient JAMAIS id, dniNie, fechaNacimiento ni dates d'audit.
 * - Le DNI/NIE et la date de naissance sont des attributs d'identité immutables.
 */
public record ClienteUpdateRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombre,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    String apellidos,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    String email,

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[6789][0-9]{8}$", message = "El teléfono debe ser un número español válido de 9 dígitos que comience por 6, 7, 8 o 9")
    @Size(max = 15, message = "El teléfono no puede superar los 15 caracteres")
    String telefono,

    @NotBlank(message = "El IBAN es obligatorio")
    @Size(max = 34, message = "El IBAN no puede superar los 34 caracteres")
    @ValidIban
    String iban
) {
}
