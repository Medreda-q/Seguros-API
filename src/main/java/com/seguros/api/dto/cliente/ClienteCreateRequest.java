package com.seguros.api.dto.cliente;

import com.seguros.api.validator.ValidDniNie;
import com.seguros.api.validator.ValidIban;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de création d'un nouveau client (POST /api/v1/clientes).
 *
 * Protection Mass Assignment :
 * Ne contient JAMAIS id, ni dates d'audit (created_at, updated_at).
 * Ils sont générés exclusivement par le serveur.
 */
public record ClienteCreateRequest(

    @NotBlank(message = "El DNI/NIE es obligatorio")
    @Size(max = 10, message = "El DNI/NIE no puede superar los 10 caracteres")
    @ValidDniNie
    String dniNie,

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

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    LocalDate fechaNacimiento,

    @NotBlank(message = "El IBAN es obligatorio")
    @Size(max = 34, message = "El IBAN no puede superar los 34 caracteres")
    @ValidIban
    String iban
) {
}
