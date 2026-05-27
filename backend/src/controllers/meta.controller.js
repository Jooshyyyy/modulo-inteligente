const Presupuesto = require('../models/presupuesto.model');
const Prediccion = require('../models/prediccion.model');

const round2 = (n) => Math.round(Number(n) * 100) / 100;

const normalizarFecha = (valor) => {
    if (!valor) return '';
    if (valor instanceof Date) {
        const y = valor.getFullYear();
        const m = String(valor.getMonth() + 1).padStart(2, '0');
        const d = String(valor.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }
    const s = String(valor);
    if (s.includes('T')) return s.slice(0, 10);
    if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10);
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

const agregarPrediccionesPorDia = (detalleDias) => {
    const map = new Map();
    for (const r of detalleDias) {
        const fecha = normalizarFecha(r.fecha_prediccion);
        if (!fecha) continue;
        if (!map.has(fecha)) {
            map.set(fecha, { total: 0, topRow: r, porCategoria: new Map() });
        }
        const ent = map.get(fecha);
        const m = Number(r.monto_proyectado || 0);
        ent.total = round2(ent.total + m);
        const cat = r.categoria_nombre || 'Otros';
        ent.porCategoria.set(cat, round2((ent.porCategoria.get(cat) || 0) + m));
        if (m > Number(ent.topRow.monto_proyectado || 0)) {
            ent.topRow = r;
        }
    }
    return map;
};

const mapPlan = async (row) => {
    if (!row) return null;
    const limites =
        row.tipo === 'CATEGORIAS'
            ? (await Presupuesto.obtenerLimitesPorPlan(row.id)).map((l) => ({
                  categoriaId: l.categoria_id,
                  categoriaNombre: l.categoria_nombre,
                  topeMensual: round2(l.tope_mensual)
              }))
            : [];

    const topeGeneral =
        row.tipo === 'GENERAL' ? round2(row.tope_mensual) : round2(limites.reduce((s, l) => s + l.topeMensual, 0));

    return {
        id: row.id,
        titulo: row.titulo,
        tipo: row.tipo,
        topeMensual: row.tipo === 'GENERAL' ? round2(row.tope_mensual) : null,
        topeTotal: topeGeneral,
        limites,
        estado: row.estado
    };
};

const nombreCatNorm = (s) => (s || 'Otros').trim().toLowerCase();

const construirEstadoPresupuesto = (plan, resumenCats, gastoProyectadoMes) => {
    const proyPorCat = new Map();
    for (const c of resumenCats) {
        proyPorCat.set(nombreCatNorm(c.categoria_nombre), Number(c.monto_total || 0));
    }

    if (plan.tipo === 'GENERAL') {
        const tope = Number(plan.topeMensual || 0);
        const gasto = round2(gastoProyectadoMes);
        const margen = round2(tope - gasto);
        const usoPct = tope > 0 ? Math.min(150, round2((gasto / tope) * 100)) : 0;
        return {
            tope,
            gasto,
            margen,
            exceso: margen < 0 ? round2(-margen) : 0,
            ahorroProyectado: margen > 0 ? margen : 0,
            usoPct,
            categoriasEstado: []
        };
    }

    const categoriasEstado = [];
    let gastoEnLimitadas = 0;
    let topeSum = 0;

    for (const lim of plan.limites) {
        const key = nombreCatNorm(lim.categoriaNombre);
        const proyectado = round2(proyPorCat.get(key) || 0);
        const tope = Number(lim.topeMensual || 0);
        const margen = round2(tope - proyectado);
        const exceso = margen < 0 ? round2(-margen) : 0;
        gastoEnLimitadas = round2(gastoEnLimitadas + proyectado);
        topeSum = round2(topeSum + tope);
        categoriasEstado.push({
            categoriaId: lim.categoriaId,
            categoriaNombre: lim.categoriaNombre,
            topeMensual: tope,
            gastoProyectado: proyectado,
            margen,
            exceso,
            usoPct: tope > 0 ? Math.min(150, round2((proyectado / tope) * 100)) : 0
        });
    }

    categoriasEstado.sort((a, b) => b.exceso - a.exceso || b.gastoProyectado - a.gastoProyectado);

    const margenTotal = round2(topeSum - gastoEnLimitadas);
    return {
        tope: topeSum,
        gasto: gastoEnLimitadas,
        margen: margenTotal,
        exceso: margenTotal < 0 ? round2(-margenTotal) : 0,
        ahorroProyectado: margenTotal > 0 ? margenTotal : 0,
        usoPct: topeSum > 0 ? Math.min(150, round2((gastoEnLimitadas / topeSum) * 100)) : 0,
        categoriasEstado
    };
};

const construirSugerencias = (plan, detalleDias, resumenCats, estado) => {
    const sugerencias = [];
    const noMitigables = ['vivienda', 'servicio', 'educación', 'educacion'];
    const esMitigable = (cat) => !noMitigables.some((n) => (cat || '').toLowerCase().includes(n));

    const limitePorCat = new Map();
    if (plan.tipo === 'CATEGORIAS') {
        for (const l of plan.limites) {
            limitePorCat.set(nombreCatNorm(l.categoriaNombre), Number(l.topeMensual || 0));
        }
    }

    let prioridad = 1;

    if (plan.tipo === 'CATEGORIAS') {
        for (const cat of estado.categoriasEstado) {
            if (cat.exceso < 5 || !esMitigable(cat.categoriaNombre)) continue;
            const reduccion = round2(cat.exceso * 0.5);
            const pct =
                cat.topeMensual > 0 ? Math.min(100, round2((reduccion / cat.exceso) * 100)) : 0;
            sugerencias.push({
                tipo: 'CATEGORIA',
                titulo: `Tope superado en ${cat.categoriaNombre}`,
                mensaje: `Proyectamos Bs. ${cat.gastoProyectado} en «${cat.categoriaNombre}» y tu tope es Bs. ${cat.topeMensual} (exceso Bs. ${cat.exceso}). Recortando la mitad del exceso liberarías Bs. ${reduccion} y volverías al ${pct}% del límite.`,
                categoria: cat.categoriaNombre,
                fecha: null,
                montoProyectado: cat.gastoProyectado,
                montoAhorroSugerido: reduccion,
                porcentajeAcercamientoMeta: pct,
                prioridad: prioridad++
            });
        }
    } else if (estado.exceso >= 5) {
        const reduccion = round2(estado.exceso * 0.3);
        const pct =
            estado.tope > 0 ? Math.min(100, round2((reduccion / estado.exceso) * 100)) : 0;
        sugerencias.push({
            tipo: 'PRESUPUESTO',
            titulo: 'Gasto total por encima del tope',
            mensaje: `El modelo proyecta Bs. ${estado.gasto} este mes y tu tope general es Bs. ${estado.tope} (exceso Bs. ${estado.exceso}). Un recorte del 30% sobre el exceso (Bs. ${reduccion}) te devolvería margen de ahorro.`,
            categoria: null,
            fecha: null,
            montoProyectado: estado.gasto,
            montoAhorroSugerido: reduccion,
            porcentajeAcercamientoMeta: pct,
            prioridad: prioridad++
        });
    }

    const porDia = agregarPrediccionesPorDia(detalleDias);
    const diasOrden = [...porDia.entries()]
        .map(([fecha, ent]) => ({ fecha, ent }))
        .filter(({ ent }) => esMitigable(ent.topRow.categoria_nombre))
        .sort((a, b) => b.ent.total - a.ent.total);

    for (const { fecha, ent } of diasOrden.slice(0, 5)) {
        const catTop = ent.topRow.categoria_nombre || 'Otros';
        let relevante = true;
        let topeRef = estado.tope;
        let proyCat = ent.total;

        if (plan.tipo === 'CATEGORIAS') {
            const topeCat = limitePorCat.get(nombreCatNorm(catTop));
            if (topeCat == null) continue;
            topeRef = topeCat;
            proyCat = ent.porCategoria.get(catTop) || ent.total;
            const catEst = estado.categoriasEstado.find(
                (c) => nombreCatNorm(c.categoriaNombre) === nombreCatNorm(catTop)
            );
            if (!catEst || catEst.exceso < 1) continue;
        } else if (estado.margen >= 0) {
            continue;
        }

        const monto = round2(ent.total);
        if (monto < 5) continue;
        const ahorro = round2(Math.min(monto * 0.2, estado.exceso > 0 ? estado.exceso * 0.15 : monto * 0.2));
        const acercamiento =
            topeRef > 0 ? Math.min(100, round2((ahorro / Math.max(1, estado.exceso || topeRef)) * 100)) : 0;

        sugerencias.push({
            tipo: 'DIA',
            titulo: 'Día pico dentro de tu presupuesto',
            mensaje: `El ${formatFechaLargaEs(fecha)} el rubro «${catTop}» concentra gasto (día Bs. ${monto}). Un recorte del 20% ese día liberaría Bs. ${ahorro} hacia tu margen de ahorro.`,
            categoria: catTop,
            fecha,
            montoProyectado: monto,
            montoAhorroSugerido: ahorro,
            porcentajeAcercamientoMeta: acercamiento,
            prioridad: prioridad++
        });
    }

    if (estado.ahorroProyectado >= 10 && sugerencias.length < 3) {
        sugerencias.push({
            tipo: 'AHORRO',
            titulo: 'Vas dentro del presupuesto',
            mensaje: `Si mantienes el ritmo proyectado (Bs. ${estado.gasto} de Bs. ${estado.tope}), podrías ahorrar Bs. ${estado.ahorroProyectado} este mes. Considerá apartar ese monto al inicio de la quincena.`,
            categoria: null,
            fecha: null,
            montoProyectado: estado.gasto,
            montoAhorroSugerido: estado.ahorroProyectado,
            porcentajeAcercamientoMeta: 100,
            prioridad: prioridad++
        });
    }

    sugerencias.sort((a, b) => {
        if (b.tipo === 'CATEGORIA' && a.tipo !== 'CATEGORIA') return 1;
        if (a.tipo === 'CATEGORIA' && b.tipo !== 'CATEGORIA') return -1;
        return b.montoAhorroSugerido - a.montoAhorroSugerido;
    });
    return sugerencias.slice(0, 8);
};

const obtenerMetaActiva = async (req, res) => {
    try {
        const row = await Presupuesto.obtenerActivo(req.usuario.id);
        if (!row) {
            return res.json({ tieneMeta: false, meta: null, plan: null });
        }
        const plan = await mapPlan(row);
        res.json({ tieneMeta: true, meta: plan, plan });
    } catch (e) {
        console.error('Plan activo:', e);
        res.status(500).json({ mensaje: 'Error al obtener el plan de presupuesto.' });
    }
};

const crearMeta = async (req, res) => {
    try {
        const { titulo, tipo, tope_mensual, limites } = req.body;
        const tipoPlan = String(tipo || '').toUpperCase();
        if (!['GENERAL', 'CATEGORIAS'].includes(tipoPlan)) {
            return res.status(400).json({
                mensaje: 'tipo debe ser GENERAL o CATEGORIAS.'
            });
        }

        const tituloFinal = (titulo && String(titulo).trim()) || 'Mi plan de ahorro';

        if (tipoPlan === 'GENERAL') {
            const tope = Number(tope_mensual);
            if (!Number.isFinite(tope) || tope <= 0) {
                return res.status(400).json({ mensaje: 'tope_mensual inválido para presupuesto general.' });
            }
            const row = await Presupuesto.crear(req.usuario.id, {
                titulo: tituloFinal.slice(0, 120),
                tipo: 'GENERAL',
                tope_mensual: tope,
                limites: []
            });
            const plan = await mapPlan(row);
            return res.status(201).json({ mensaje: 'Plan de presupuesto activado.', meta: plan, plan });
        }

        const lista = Array.isArray(limites) ? limites : [];
        if (lista.length === 0) {
            return res.status(400).json({
                mensaje: 'Indica al menos una categoría con tope_mensual.'
            });
        }
        const normalizados = [];
        for (const item of lista) {
            const cid = parseInt(item.categoria_id, 10);
            const tope = Number(item.tope_mensual);
            if (!Number.isFinite(cid) || !Number.isFinite(tope) || tope <= 0) continue;
            normalizados.push({ categoria_id: cid, tope_mensual: tope });
        }
        if (normalizados.length === 0) {
            return res.status(400).json({ mensaje: 'Límites por categoría inválidos.' });
        }

        const row = await Presupuesto.crear(req.usuario.id, {
            titulo: tituloFinal.slice(0, 120),
            tipo: 'CATEGORIAS',
            tope_mensual: null,
            limites: normalizados
        });
        const plan = await mapPlan(row);
        res.status(201).json({ mensaje: 'Límites por categoría activados.', meta: plan, plan });
    } catch (e) {
        console.error('Crear plan:', e);
        res.status(500).json({ mensaje: 'Error al guardar el plan.' });
    }
};

const actualizarProgreso = async (req, res) => {
    res.status(410).json({
        mensaje: 'El progreso manual ya no aplica: el ahorro se mide contra el presupuesto y las predicciones.'
    });
};

const pausarMeta = async (req, res) => {
    try {
        const planId = parseInt(req.params.id, 10);
        const row = await Presupuesto.pausar(req.usuario.id, planId);
        if (!row) {
            return res.status(404).json({ mensaje: 'Plan no encontrado.' });
        }
        res.json({ mensaje: 'Plan de presupuesto pausado.' });
    } catch (e) {
        console.error('Pausar plan:', e);
        res.status(500).json({ mensaje: 'Error al pausar el plan.' });
    }
};

const obtenerIaCoach = async (req, res) => {
    try {
        const usuarioId = req.usuario.id;
        const mes = req.query.mes || new Date().toISOString().slice(0, 7);

        const [planRow, resumenCats, detalleDias] = await Promise.all([
            Presupuesto.obtenerActivo(usuarioId),
            Prediccion.obtenerMesResumen(usuarioId, mes),
            Prediccion.obtenerMesPorDia(usuarioId, mes)
        ]);

        const porDiaMap = agregarPrediccionesPorDia(detalleDias);
        const gastoProyectadoMes = round2(
            [...porDiaMap.values()].reduce((s, ent) => s + ent.total, 0)
        );
        const diasConPrediccion = porDiaMap.size;

        if (!planRow) {
            return res.json({
                tieneMeta: false,
                meta: null,
                plan: null,
                mes,
                gastoProyectadoMes,
                diasConPrediccion,
                narrativa:
                    'Definí un tope de gasto mensual (general) o límites en categorías que elijas. El coach comparará tus predicciones con ese presupuesto y te dirá dónde recortar para ahorrar.',
                indicadores: [],
                sugerencias: [],
                estadoPresupuesto: null
            });
        }

        const plan = await mapPlan(planRow);
        const estado = construirEstadoPresupuesto(plan, resumenCats, gastoProyectadoMes);

        let narrativa = `Plan «${plan.titulo}» (`;
        narrativa +=
            plan.tipo === 'GENERAL'
                ? `tope mensual Bs. ${estado.tope})`
                : `${plan.limites.length} categoría(s), tope combinado Bs. ${estado.tope})`;
        narrativa += `. `;

        if (diasConPrediccion === 0) {
            narrativa +=
                'Sin predicciones este mes: ejecutá el generador de IA para ver margen de ahorro y alertas.';
        } else {
            const gastoRef =
                plan.tipo === 'CATEGORIAS' ? estado.gasto : gastoProyectadoMes;
            narrativa += `Gasto proyectado en alcance: Bs. ${gastoRef}. `;
            if (estado.exceso > 0) {
                narrativa += `Vas Bs. ${estado.exceso} por encima del presupuesto — el coach prioriza recortes ahí.`;
            } else {
                narrativa += `Margen de ahorro proyectado: Bs. ${estado.ahorroProyectado} (${estado.usoPct}% del tope usado).`;
            }
        }

        const indicadores = [
            {
                clave: 'tope',
                etiqueta: plan.tipo === 'GENERAL' ? 'Tope mensual' : 'Tope en categorías elegidas',
                valor: `Bs. ${estado.tope}`,
                detalle: plan.tipo === 'GENERAL' ? 'Presupuesto total del mes' : 'Suma de tus límites por rubro'
            },
            {
                clave: 'gasto_proyectado',
                etiqueta: 'Gasto proyectado (alcance)',
                valor: `Bs. ${plan.tipo === 'CATEGORIAS' ? estado.gasto : gastoProyectadoMes}`,
                detalle: 'Según predicciones del modelo'
            },
            {
                clave: 'margen',
                etiqueta: estado.exceso > 0 ? 'Exceso sobre tope' : 'Ahorro proyectado',
                valor: `Bs. ${estado.exceso > 0 ? estado.exceso : estado.ahorroProyectado}`,
                detalle: `${estado.usoPct}% del presupuesto comprometido`
            }
        ];

        const sugerencias =
            diasConPrediccion > 0
                ? construirSugerencias(plan, detalleDias, resumenCats, estado)
                : [];

        res.json({
            tieneMeta: true,
            meta: plan,
            plan,
            mes,
            gastoProyectadoMes,
            diasConPrediccion,
            narrativa: narrativa.trim(),
            indicadores,
            sugerencias,
            estadoPresupuesto: estado
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
