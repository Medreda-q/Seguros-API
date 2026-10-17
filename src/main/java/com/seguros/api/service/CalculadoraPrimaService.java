package com.seguros.api.service;

import com.seguros.api.entity.enums.TipoPoliza;
import com.seguros.api.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * Service actuariel responsable de la tarification de la prime annuelle (primaAnual).
 * Respecte strictement la Règle Métier 4.1 du document fonctionnel :
 * - Calcul de l'âge en années révolues à la date d'effet (fechaInicio).
 * - Utilisation exclusive de BigDecimal avec RoundingMode.HALF_UP et échelle à 2 décimales.
 */
@Service
public class CalculadoraPrimaService {

    // Tarifs de base par type de contrat (Règle 4.1.A)
    public static final BigDecimal BASE_AUTO = new BigDecimal("500.00");
    public static final BigDecimal BASE_HOGAR = new BigDecimal("300.00");
    public static final BigDecimal BASE_VIDA = new BigDecimal("200.00");

    // Facteurs actuariels AUTO (Règle 4.1.B)
    public static final BigDecimal FACTOR_AUTO_JOVEN = new BigDecimal("1.50");     // edad < 25
    public static final BigDecimal FACTOR_AUTO_ESTANDAR = new BigDecimal("1.00");  // 25 <= edad <= 65
    public static final BigDecimal FACTOR_AUTO_SENIOR = new BigDecimal("1.20");    // edad > 65

    // Facteurs actuariels VIDA (Règle 4.1.B)
    public static final BigDecimal FACTOR_VIDA_JOVEN = new BigDecimal("1.00");     // edad < 35
    public static final BigDecimal FACTOR_VIDA_INTERMEDIO = new BigDecimal("1.30"); // 35 <= edad <= 55
    public static final BigDecimal FACTOR_VIDA_SENIOR = new BigDecimal("1.80");    // edad > 55

    // Facteur HOGAR (Règle 4.1.B)
    public static final BigDecimal FACTOR_HOGAR = new BigDecimal("1.00");          // indépendamment de l'âge

    /**
     * Calcule la prime annuelle pour un type de contrat et un profil client donnés.
     *
     * @param tipo Type de police (AUTO, HOGAR, VIDA)
     * @param fechaNacimiento Date de naissance du titulaire
     * @param fechaInicio Date d'effet de la police
     * @return Prime annuelle calculée en BigDecimal arrondie à 2 décimales (HALF_UP)
     */
    public BigDecimal calcularPrima(TipoPoliza tipo, LocalDate fechaNacimiento, LocalDate fechaInicio) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de póliza no puede ser nulo");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser nula");
        }
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio de la póliza no puede ser nula");
        }
        if (fechaNacimiento.isAfter(fechaInicio)) {
            throw new BusinessRuleException("La fecha de nacimiento no puede ser posterior a la fecha de inicio de la póliza");
        }

        int edad = Period.between(fechaNacimiento, fechaInicio).getYears();
        BigDecimal base = obtenerTarifaBase(tipo);
        BigDecimal factor = obtenerFactorEdad(tipo, edad);

        return base.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule l'âge révolu en années entre la date de naissance et la date de début.
     */
    public int calcularEdad(LocalDate fechaNacimiento, LocalDate fechaInicio) {
        return Period.between(fechaNacimiento, fechaInicio).getYears();
    }

    private BigDecimal obtenerTarifaBase(TipoPoliza tipo) {
        return switch (tipo) {
            case AUTO -> BASE_AUTO;
            case HOGAR -> BASE_HOGAR;
            case VIDA -> BASE_VIDA;
        };
    }

    private BigDecimal obtenerFactorEdad(TipoPoliza tipo, int edad) {
        return switch (tipo) {
            case AUTO -> {
                if (edad < 25) {
                    yield FACTOR_AUTO_JOVEN;
                } else if (edad <= 65) {
                    yield FACTOR_AUTO_ESTANDAR;
                } else {
                    yield FACTOR_AUTO_SENIOR;
                }
            }
            case VIDA -> {
                if (edad < 35) {
                    yield FACTOR_VIDA_JOVEN;
                } else if (edad <= 55) {
                    yield FACTOR_VIDA_INTERMEDIO;
                } else {
                    yield FACTOR_VIDA_SENIOR;
                }
            }
            case HOGAR -> FACTOR_HOGAR;
        };
    }
}
