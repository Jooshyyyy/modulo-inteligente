const Meta = require('../models/meta.model');
const Prediccion = require('../models/prediccion.model');

const round2 = (n) => Math.round(Number(n) * 100) / 100;

const formatFechaLargaEs = (isoDateStr) => {
    if (!isoDateStr) return '';
    const s = String(isoDateStr).slice(0, 10);
    const [y, m, d] = s.split('-').map((x) => parseInt(x, 10));
    const date = new Date(y, m - 1, d);
    return date.toLocaleDateString('es-BO', {
        weekday: 'long',
        day: 'numeric',
        month: 'long'
    });
};

const diasHasta = (fechaLimiteStr) => {
    const s = String(fechaLimiteStr).slice(0, 10);
    const [y, m, d] = s.split('-').map((x) => parseInt(x, 10));
    const lim = new Date(y, m - 1, d);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    lim.setHours(0, 0, 0, 0);
    const ms = lim.getTime() - hoy.getTime();
    return Math.max(1, Math.ceil(ms / 86400000));
};

const mapMeta = (row) => {
    if (!row) return null;
    const objetivo = Number(row.monto_objetivo);
    const acum = Number(row.monto_acumulado || 0);
    const restante = Math.max(0, round2(objetivo - acum));
    const pct = objetivo > 0 ? Math.min(100, round2((acum / objetivo) * 100)) : 0;
    return {
        id: row.id,
        titulo: row.titulo,
        descripcion: row.descripcion,
        plantilla: row.plantilla,
        montoObjetivo: round2(objetivo),
        montoAcumulado: round2(acum),
        montoRestante: restante,
        porcentajeCompletado: pct,
        fechaLimite: String(row.fecha_limite).slice(0, 10),
        estado: row.estado
    };
};

const construirSugerencias = (detalleDias, resumenCats, montoRestanteMeta) => {
    const sugerencias = [];
    const diasOrden = [...detalleDias].sort(
        (a, b) => Number(b.monto_proyectado || 0) - Number(a.monto_proyectado || 0)
    );

    const vistos = new Set();
    let prioridad = 1;
    for (const row of diasOrden) {
        const fecha = String(row.fecha_prediccion).slice(0, 10);
        if (vistos.has(fecha)) continue;
        vistos.add(fecha);
        if (vistos.size > 6) break;

        const monto = Number(row.monto_proyectado || 0);
        if (monto < 5) continue;

        const reduccion = 0.2;
        const ahorro = round2(monto * reduccion);
        const acercamiento =
            montoRestanteMeta > 0
                ? Math.min(100, round2((ahorro / montoRestanteMeta) * 100))
                : 0;

        const etiquetaFecha = formatFechaLargaEs(fecha);
        sugerencias.push({
            tipo: 'DIA',
            titulo: 'Recorte en día pico proyectado',
            mensaje: `Si el ${etiquetaFecha} reduces un 20% el gasto en «${row.categoria_nombre}» (proyectado Bs. ${round2(
                monto
            )}), ahorrarías Bs. ${ahorro} y estarías un ${acercamiento}% más cerca de tu meta.`,
            categoria: row.categoria_nombre,
            fecha,
            montoProyectado: round2(monto),
            montoAhorroSugerido: ahorro,
            porcentajeAcercamientoMeta: acercamiento,
            prioridad: prioridad++
        });
    }

    const cats = [...resumenCats].sort(
        (a, b) => Number(b.monto_total || 0) - Number(a.monto_total || 0)
    );
    for (let i = 0; i < Math.min(3, cats.length); i++) {
        const c = cats[i];
        const totalCat = Number(c.monto_total || 0);
        if (totalCat < 10) continue;
        const ahorro = round2(totalCat * 0.1);
        const acercamiento =
            montoRestanteMeta > 0
                ? Math.min(100, round2((ahorro / montoRestanteMeta) * 100))
                : 0;
        sugerencias.push({
            tipo: 'CATEGORIA',
            titulo: 'Menos presión en una categoría',
            mensaje: `Bajando un 10% lo proyectado en «${c.categoria_nombre}» este mes (Bs. ${round2(
                totalCat
            )}), liberarías Bs. ${ahorro} (~${acercamiento}% más cerca de tu meta).`,
            categoria: c.categoria_nombre,
            fecha: null,
            montoProyectado: round2(totalCat),
            montoAhorroSugerido: ahorro,
            porcentajeAcercamientoMeta: acercamiento,
            prioridad: prioridad++
        });
    }

    sugerencias.sort(
        (a, b) => b.porcentajeAcercamientoMeta - a.porcentajeAcercamientoMeta
    );
    return sugerencias.slice(0, 8);
};

const obtenerMetaActiva = async (req, res) => {
    try {
        const row = await Meta.obtenerActiva(req.usuario.id);
        if (!row) {
            return res.json({ tieneMeta: false, meta: null });
        }
        res.json({ tieneMeta: true, meta: mapMeta(row) });
    } catch (e) {
        console.error('Meta activa:', e);
        res.status(500).json({ mensaje: 'Error al obtener la meta.' });
    }
};

const crearMeta = async (req, res) => {
    try {
        const { titulo, monto_objetivo, fecha_limite, plantilla, descripcion } = req.body;
        if (!titulo || monto_objetivo == null || !fecha_limite) {
            return res.status(400).json({
                mensaje: 'titulo, monto_objetivo y fecha_limite son obligatorios.'
            });
        }
        const monto = Number(monto_objetivo);
        if (!Number.isFinite(monto) || monto <= 0) {
            return res.status(400).json({ mensaje: 'monto_objetivo inválido.' });
        }

        await Meta.pausarActivas(req.usuario.id);
        const row = await Meta.crear(req.usuario.id, {
            titulo: String(titulo).slice(0, 120),
            descripcion: descripcion ? String(descripcion).slice(0, 2000) : null,
            plantilla: plantilla ? String(plantilla).slice(0, 40) : null,
            monto_objetivo: monto,
            fecha_limite: String(fecha_limite).slice(0, 10)
        });

        res.status(201).json({ mensaje: 'Meta creada.', meta: mapMeta(row) });
    } catch (e) {
        console.error('Crear meta:', e);
        res.status(500).json({ mensaje: 'Error al crear la meta.' });
    }
};

const actualizarProgreso = async (req, res) => {
    try {
        const metaId = parseInt(req.params.id, 10);
        const { monto_acumulado } = req.body;
        if (!Number.isFinite(metaId) || monto_acumulado == null) {
            return res.status(400).json({ mensaje: 'Datos inválidos.' });
        }
        const ac = Math.max(0, Number(monto_acumulado));
        const row = await Meta.actualizarAcumulado(req.usuario.id, metaId, ac);
        if (!row) {
            return res.status(404).json({ mensaje: 'Meta no encontrada.' });
        }
        res.json({ mensaje: 'Progreso actualizado.', meta: mapMeta(row) });
    } catch (e) {
        console.error('Progreso meta:', e);
        res.status(500).json({ mensaje: 'Error al actualizar progreso.' });
    }
};

const pausarMeta = async (req, res) => {
    try {
        const metaId = parseInt(req.params.id, 10);
        const row = await Meta.pausar(req.usuario.id, metaId);
        if (!row) {
            return res.status(404).json({ mensaje: 'Meta no encontrada.' });
        }
        res.json({ mensaje: 'Meta pausada.' });
    } catch (e) {
        console.error('Pausar meta:', e);
        res.status(500).json({ mensaje: 'Error al pausar la meta.' });
    }
};

const obtenerIaCoach = async (req, res) => {
    try {
        const usuarioId = req.usuario.id;
        const mes = req.query.mes || new Date().toISOString().slice(0, 7);

        const [metaRow, resumenCats, detalleDias] = await Promise.all([
            Meta.obtenerActiva(usuarioId),
            Prediccion.obtenerMesResumen(usuarioId, mes),
            Prediccion.obtenerMesPorDia(usuarioId, mes)
        ]);

        const gastoProyectadoMes = round2(
            detalleDias.reduce((s, r) => s + Number(r.monto_proyectado || 0), 0)
        );

        if (!metaRow) {
            return res.json({
                tieneMeta: false,
                meta: null,
                mes,
                gastoProyectadoMes,
                diasConPrediccion: detalleDias.length,
                narrativa:
                    'Aún no tienes una meta activa. Cuando la definas, el coach cruza tus gastos proyectados con tu objetivo y te dirá qué día o categoría conviene recortar para acercarte con porcentajes concretos.',
                indicadores: [],
                sugerencias: []
            });
        }

        const meta = mapMeta(metaRow);
        const montoRestante = meta.montoRestante;
        const diasRest = diasHasta(meta.fechaLimite);
        const ritmoDiario = montoRestante > 0 ? round2(montoRestante / diasRest) : 0;

        const topDia = detalleDias.reduce(
            (best, r) => {
                const m = Number(r.monto_proyectado || 0);
                return m > best.m ? { m, r } : best;
            },
            { m: 0, r: null }
        );

        let narrativa = `Tu meta «${meta.titulo}» va al ${meta.porcentajeCompletado}% — faltan Bs. ${montoRestante} antes del ${meta.fechaLimite}. `;
        if (detalleDias.length === 0) {
            narrativa +=
                'No hay predicciones guardadas para este mes; ejecuta el generador de predicciones para ver acciones personalizadas.';
        } else {
            narrativa += `El modelo proyecta unos Bs. ${gastoProyectadoMes} de gasto en ${mes}. `;
            if (topDia.r) {
                narrativa += `El pico más alto aparece el ${formatFechaLargaEs(
                    String(topDia.r.fecha_prediccion).slice(0, 10)
                )} en «${topDia.r.categoria_nombre}».`;
            }
        }

        const indicadores = [
            {
                clave: 'ritmo_diario',
                etiqueta: 'Ritmo diario hacia la meta',
                valor: `Bs. ${ritmoDiario}`,
                detalle: `Con ${diasRest} días hasta tu fecha límite`
            },
            {
                clave: 'gasto_proyectado_mes',
                etiqueta: 'Gasto proyectado (mes)',
                valor: `Bs. ${gastoProyectadoMes}`,
                detalle: 'Suma de predicciones diarias del modelo'
            }
        ];

        const sugerencias =
            detalleDias.length > 0
                ? construirSugerencias(detalleDias, resumenCats, montoRestante)
                : [];

        res.json({
            tieneMeta: true,
            meta,
            mes,
            gastoProyectadoMes,
            diasConPrediccion: detalleDias.length,
            narrativa: narrativa.trim(),
            indicadores,
            sugerencias
        });
    } catch (e) {
        console.error('IA coach:', e);
        res.status(500).json({ mensaje: 'Error al generar el coach de IA.' });
    }
};

module.exports = {
    obtenerMetaActiva,
    crearMeta,
    actualizarProgreso,
    pausarMeta,
    obtenerIaCoach
};
