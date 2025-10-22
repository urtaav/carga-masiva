-- Tabla de Sueldos
CREATE TABLE IF NOT EXISTS sueldos (
    id BIGSERIAL PRIMARY KEY,
    numero_empleado VARCHAR(50) NOT NULL,
    nombre_completo VARCHAR(200) NOT NULL,
    puesto VARCHAR(100) NOT NULL,
    salario_base DECIMAL(12, 2) NOT NULL,
    bonos DECIMAL(12, 2) DEFAULT 0,
    deducciones DECIMAL(12, 2) DEFAULT 0,
    salario_neto DECIMAL(12, 2) NOT NULL,
    periodo_pago VARCHAR(20) NOT NULL,
    fecha_pago DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_empleado_periodo UNIQUE (numero_empleado, periodo_pago)
);

CREATE INDEX idx_sueldos_empleado ON sueldos(numero_empleado);
CREATE INDEX idx_sueldos_fecha ON sueldos(fecha_pago);
CREATE INDEX idx_sueldos_periodo ON sueldos(periodo_pago);

-- Tabla de Jobs de Importación
CREATE TABLE IF NOT EXISTS importacion_jobs (
    id VARCHAR(36) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_registros INTEGER DEFAULT 0,
    procesados INTEGER DEFAULT 0,
    exitosos INTEGER DEFAULT 0,
    errores INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_jobs_status ON importacion_jobs(status);
CREATE INDEX idx_jobs_email ON importacion_jobs(user_email);
CREATE INDEX idx_jobs_created ON importacion_jobs(created_at);

-- Tabla de Errores de Importación
CREATE TABLE IF NOT EXISTS importacion_errores (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    row_number INTEGER NOT NULL,
    numero_empleado VARCHAR(50),
    error_message TEXT NOT NULL,
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES importacion_jobs(id) ON DELETE CASCADE
);

CREATE INDEX idx_errores_job ON importacion_errores(job_id);