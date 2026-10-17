package com.seguros.api.entity.enums;

/**
 * États de la machine à états d'un sinistre (workflow strictement orienté).
 */
public enum EstadoSiniestro {
    ABIERTO,
    EN_REVISION,
    APROBADO,
    RECHAZADO
}
