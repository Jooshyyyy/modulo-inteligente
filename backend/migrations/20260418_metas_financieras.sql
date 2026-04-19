-- Metas de ahorro vinculadas al usuario; el coach de IA usa predicciones + meta activa
CREATE TABLE IF NOT EXISTS public.metas_financieras (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL REFERENCES public.usuarios (id) ON DELETE CASCADE,
    titulo VARCHAR(120) NOT NULL,
    descripcion TEXT NULL,
    plantilla VARCHAR(40) NULL,
    monto_objetivo NUMERIC(12, 2) NOT NULL CHECK (monto_objetivo > 0),
    monto_acumulado NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (monto_acumulado >= 0),
    fecha_limite DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    creado_en TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITHOUT TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_metas_usuario_estado
    ON public.metas_financieras (usuario_id, estado);
