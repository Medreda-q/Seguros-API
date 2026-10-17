package com.seguros.api.entity;

import com.seguros.api.entity.enums.EstadoSiniestro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité JPA représentant une déclaration de sinistre (claim).
 * Mappée sur la table 'siniestros'.
 */
@Entity
@Table(name = "siniestros")
public class Siniestro extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 50)
    private String numero;

    // FetchType.LAZY pour ne pas charger toute la police et le client par défaut
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_ocurrencia", nullable = false)
    private LocalDate fechaOcurrencia;

    // Précision monétaire exacte avec BigDecimal
    @Column(name = "importe_reclamado", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeReclamado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSiniestro estado;

    public Siniestro() {
    }

    public Siniestro(String numero, Poliza poliza, String descripcion, LocalDate fechaOcurrencia, BigDecimal importeReclamado, EstadoSiniestro estado) {
        this.numero = numero;
        this.poliza = poliza;
        this.descripcion = descripcion;
        this.fechaOcurrencia = fechaOcurrencia;
        this.importeReclamado = importeReclamado;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Poliza getPoliza() {
        return poliza;
    }

    public void setPoliza(Poliza poliza) {
        this.poliza = poliza;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFechaOcurrencia() {
        return fechaOcurrencia;
    }

    public void setFechaOcurrencia(LocalDate fechaOcurrencia) {
        this.fechaOcurrencia = fechaOcurrencia;
    }

    public BigDecimal getImporteReclamado() {
        return importeReclamado;
    }

    public void setImporteReclamado(BigDecimal importeReclamado) {
        this.importeReclamado = importeReclamado;
    }

    public EstadoSiniestro getEstado() {
        return estado;
    }

    public void setEstado(EstadoSiniestro estado) {
        this.estado = estado;
    }

    // Égalité basée sur le numéro unique de sinistre
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Siniestro siniestro = (Siniestro) o;
        return Objects.equals(numero, siniestro.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }
}
