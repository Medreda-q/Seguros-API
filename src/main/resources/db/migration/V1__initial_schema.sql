-- ==============================================================================
-- V1__initial_schema.sql : Schéma initial pour SegurosAPI (Fase 1 - Core API)
-- Tables : clientes, polizas, siniestros (usuarios et audit_logs en Phase 2)
-- SGBD : PostgreSQL 16
-- ==============================================================================

-- 1. Table des Clients (Tomadores de seguro)
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    dni_nie VARCHAR(10) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefono VARCHAR(15) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    iban VARCHAR(34) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_clientes_dni_nie UNIQUE (dni_nie)
);

CREATE INDEX idx_clientes_dni_nie ON clientes(dni_nie);
CREATE INDEX idx_clientes_email ON clientes(email);

-- 2. Table des Polices d'assurance (Polizas)
CREATE TABLE polizas (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    prima_anual NUMERIC(12, 2) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    cliente_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_polizas_numero UNIQUE (numero),
    CONSTRAINT fk_polizas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_polizas_tipo CHECK (tipo IN ('AUTO', 'HOGAR', 'VIDA')),
    CONSTRAINT chk_polizas_estado CHECK (estado IN ('ACTIVA', 'SUSPENDIDA', 'CANCELADA')),
    CONSTRAINT chk_polizas_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_polizas_prima CHECK (prima_anual > 0)
);

CREATE INDEX idx_polizas_cliente_id ON polizas(cliente_id);
CREATE INDEX idx_polizas_numero ON polizas(numero);
CREATE INDEX idx_polizas_estado ON polizas(estado);

-- 3. Table des Sinistres (Siniestros)
CREATE TABLE siniestros (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(50) NOT NULL,
    poliza_id BIGINT NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_ocurrencia DATE NOT NULL,
    importe_reclamado NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_siniestros_numero UNIQUE (numero),
    CONSTRAINT fk_siniestros_poliza FOREIGN KEY (poliza_id) REFERENCES polizas(id) ON DELETE RESTRICT,
    CONSTRAINT chk_siniestros_estado CHECK (estado IN ('ABIERTO', 'EN_REVISION', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT chk_siniestros_importe CHECK (importe_reclamado > 0)
);

CREATE INDEX idx_siniestros_poliza_id ON siniestros(poliza_id);
CREATE INDEX idx_siniestros_numero ON siniestros(numero);
CREATE INDEX idx_siniestros_estado ON siniestros(estado);
