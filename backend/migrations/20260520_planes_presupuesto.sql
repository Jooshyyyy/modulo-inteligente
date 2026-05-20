-- Plan de ahorro basado en presupuesto (general o límites por categoría)
CREATE TABLE IF NOT EXISTS public.planes_presupuesto (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL REFERENCES public.usuarios (id) ON DELETE CASCADE,
    titulo VARCHAR(120) NOT NULL DEFAULT 'Mi plan de ahorro',
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('GENERAL', 'CATEGORIAS')),
    tope_mensual NUMERIC(12, 2) NULL CHECK (tope_mensual IS NULL OR tope_mensual > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    creado_en TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITHOUT TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_planes_presupuesto_usuario
    ON public.planes_presupuesto (usuario_id, estado);

CREATE TABLE IF NOT EXISTS public.presupuesto_categoria_limites (
    id SERIAL PRIMARY KEY,
    plan_id INT NOT NULL REFERENCES public.planes_presupuesto (id) ON DELETE CASCADE,
    categoria_id INT NOT NULL REFERENCES public.categorias (id),
    tope_mensual NUMERIC(12, 2) NOT NULL CHECK (tope_mensual > 0),
    UNIQUE (plan_id, categoria_id)
);

CREATE INDEX IF NOT EXISTS idx_presupuesto_cat_plan
    ON public.presupuesto_categoria_limites (plan_id);
