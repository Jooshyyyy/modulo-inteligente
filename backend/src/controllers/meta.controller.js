const Meta = require('../models/meta.model');
const Prediccion = require('../models/prediccion.model');

const round2 = (n) => Math.round(Number(n) * 100) / 100;

/**
 * Normaliza un valor de fecha que puede venir como objeto Date de PostgreSQL
 * o como string ISO, y devuelve siempre "YYYY-MM-DD"
 */
const normalizarFecha = (valor) => {
    if (!valor) return '';
    if (valor instanceof Date) {
        // pg devuelve Date objects para columnas tipo date
        const y = valor.getFullYear();
        const m = String(valor.getMonth() + 1).padStart(2, '0');
        const d = String(valor.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }
    const s = String(valor);
    // Si ya es ISO (contiene 'T' o tiene formato yyyy-mm-dd)
    if (s.includes('T')) return s.slice(0, 10);
    if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10);
    // Fallback: intentar parsear
    const dt = new Date(s);
    if (isNaN(dt.getTime())) return '';
    const y = dt.getFullYear();
    const mo = String(dt.getMonth() + 1).padStart(2, '0');
    const dy = String(dt.getDate()).padStart(2, '0');
    return `${y}-${mo}-${dy}`;
};

const formatFechaLargaEs = (valor) => {
    const s = normalizarFecha(valor);
    if (!s) return 'fecha desconocida';
    const [y, m, d] = s.split('-').map((x) => parseInt(x, 10));
    const date = new Date(y, m - 1, d);
    return date.toLocaleDateString('es-BO', {
        weekday: 'long',
        day: 'numeric',
        month: 'long'
    });
};

const diasHasta = (fechaLimiteStr) => {
    const s = normalizarFecha(fechaLimiteStr);
    if (!s) return 1;
    const [y, m, d] = s.split('-').map((x) => parseInt(x, 10));
    const lim = new Date(y, m - 1, d);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    lim.setHours(0, 0, 0, 0);
    const ms = lim.getTime() - hoy.getTime();
    return Math.max(1, Math.ceil(ms / 86400000));
};

/** Agrupa varias predicciones del mismo día (reparto por categoría) en un total y rubro principal. */
const agregarPrediccionesPorDia = (detalleDias) => {
    const map = new Map();
    for (const r of detalleDias) {
        const fecha = normalizarFecha(r.fecha_prediccion);
        if (!fecha) continue;
        if (!map.has(fecha)) {
            map.set(fecha, { total: 0, topRow: r });
        }
        const ent = map.get(fecha);
        const m = Number(r.monto_proyectado || 0);
        ent.total = round2(ent.total + m);
        if (m > Number(ent.topRow.monto_proyectado || 0)) {
            ent.topRow = r;
        }
    }
    return map;
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
        fechaLimite: normalizarFecha(row.fecha_limite) || 'sin fecha',
        estado: row.estado
    };
};

const construirSugerencias = (detalleDias, resumenCats, montoRestanteMeta) => {
    const sugerencias = [];
    const porDia = agregarPrediccionesPorDia(detalleDias);
    
    const noMitigables = ['vivienda', 'servicio', 'educación', 'educacion'];
    const esMitigable = (cat) => !noMitigables.some(n => (cat || '').toLowerCase().includes(n));

    const diasOrden = [...porDia.entries()]
        .map(([fecha, ent]) => ({
            fecha_prediccion: fecha,
            categoria_nombre: ent.topRow.categoria_nombre,
            monto_proyectado: ent.total
        }))
        .filter(row => esMitigable(row.categoria_nombre))
        .sort((a, b) => Number(b.monto_proyectado || 0) - Number(a.monto_proyectado || 0));

    let prioridad = 1;
    for (const row of diasOrden.slice(0, 6)) {
        const fecha = normalizarFecha(row.fecha_prediccion);
        if (!fecha) continue;
        const monto = Number(row.monto_proyectado || 0);
        if (monto < 5) continue;

        const reduccion = 0.2;
        const ahorro = round2(monto * reduccion);
        const acercamiento =
            montoRestanteMeta > 0
                ? Math.min(100, round2((ahorro / montoRestanteMeta) * 100))
                : 0;

        const etiquetaFecha = formatFechaLargaEs(fecha);
        let sugerenciaAdicional = '';
        const catLow = (row.categoria_nombre || '').toLowerCase();
        if (catLow.includes('alimentación') || catLow.includes('alimentacion')) {
            sugerenciaAdicional = ' (ej. cocinando en casa)';
        } else if (catLow.includes('entretenimiento')) {
            sugerenciaAdicional = ' (ej. buscando opciones gratis)';
        }

        sugerencias.push({
            tipo: 'DIA',
            titulo: 'Recorte en día pico proyectado',
            mensaje: `Si el ${etiquetaFecha} reduces un 20% el gasto total proyectado (rubro principal «${row.categoria_nombre}»${sugerenciaAdicional}, día Bs. ${round2(
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

    const cats = [...resumenCats]
        .filter(c => esMitigable(c.categoria_nombre))
        .sort((a, b) => Number(b.monto_total || 0) - Number(a.monto_total || 0));
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

        const porDiaMap = agregarPrediccionesPorDia(detalleDias);
        const gastoProyectadoMes = round2(
            [...porDiaMap.values()].reduce((s, ent) => s + ent.total, 0)
        );
        const diasConPrediccion = porDiaMap.size;

        if (!metaRow) {
            return res.json({
                tieneMeta: false,
                meta: null,
                mes,
                gastoProyectadoMes,
                diasConPrediccion,
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

        let topDia = { m: 0, r: null };
        for (const [fecha, ent] of porDiaMap) {
            if (ent.total > topDia.m) {
                topDia = {
                    m: ent.total,
                    r: {
                        fecha_prediccion: fecha,
                        categoria_nombre: ent.topRow.categoria_nombre
                    }
                };
            }
        }

        const pctDisplay = isNaN(meta.porcentajeCompletado) ? '0' : String(round2(meta.porcentajeCompletado));
        const montoRestanteDisplay = isNaN(montoRestante) ? '0.00' : String(round2(montoRestante));
        const fechaLimiteDisplay = meta.fechaLimite ? String(meta.fechaLimite).slice(0, 10) : 'sin fecha';
        let narrativa = `Tu meta «${meta.titulo}» va al ${pctDisplay}% — faltan Bs. ${montoRestanteDisplay} antes del ${fechaLimiteDisplay}. `;
        if (diasConPrediccion === 0) {
            narrativa +=
                'No hay predicciones guardadas para este mes; ejecuta el generador de predicciones para ver acciones personalizadas.';
        } else {
            const gpDisplay = isNaN(gastoProyectadoMes) ? '0.00' : String(gastoProyectadoMes);
            narrativa += `El modelo proyecta unos Bs. ${gpDisplay} de gasto en ${mes}. `;
            if (topDia.r) {
                narrativa += `El pico más alto aparece el ${formatFechaLargaEs(
                    String(topDia.r.fecha_prediccion).slice(0, 10)
                )} en «${topDia.r.categoria_nombre}».`;
            }
        }

        const ritmoDiarioDisplay = isNaN(ritmoDiario) ? '0.00' : String(ritmoDiario);
        const gpDisplay2 = isNaN(gastoProyectadoMes) ? '0.00' : String(gastoProyectadoMes);
        const indicadores = [
            {
                clave: 'ritmo_diario',
                etiqueta: 'Ritmo diario hacia la meta',
                valor: `Bs. ${ritmoDiarioDisplay}`,
                detalle: `Con ${diasRest} días hasta tu fecha límite`
            },
            {
                clave: 'gasto_proyectado_mes',
                etiqueta: 'Gasto proyectado (mes)',
                valor: `Bs. ${gpDisplay2}`,
                detalle: 'Suma de predicciones diarias del modelo'
            }
        ];

        const sugerencias =
            diasConPrediccion > 0
                ? construirSugerencias(detalleDias, resumenCats, montoRestante)
                : [];

        res.json({
            tieneMeta: true,
            meta,
            mes,
            gastoProyectadoMes,
            diasConPrediccion,
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
