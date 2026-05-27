const Movimiento = require('../models/movimiento.model');
const Cuenta = require('../models/cuenta.model');
const Categoria = require('../models/categoria.model');

const normalizarTexto = (v) => (v ?? "").toString().trim();

/**
 * Heurísticas rápidas (sin IA) para conceptos evidentes.
 * Devuelve el NOMBRE de la categoría (como está en la tabla `categorias`) o null.
 */
const categoriaPorHeuristica = (concepto) => {
    const t = normalizarTexto(concepto).toLowerCase();
    if (!t) return null;

    // Alimentación
    if (
        /\b(hamburguesa|hamburguesas|comida|almuerzo|cena|desayuno|snack|pizza|pollo|carne|restaurante|restaurant|burger|mcdonald|subway|super|supermercado|mercado|pan|refresco|gaseosa)\b/i.test(t)
    ) return 'Alimentación';

    // Transporte
    if (/\b(taxi|trufi|micro|bus|pasaje|uber|didi|gasolina|diesel|combustible|parqueo)\b/i.test(t)) return 'Transporte';

    // Vivienda / Servicios
    if (/\b(alquiler|renta|expensa|hipoteca)\b/i.test(t)) return 'Vivienda';
    if (/\b(luz|agua|internet|wifi|telefono|tel[eé]fono|tigo|entel|viva)\b/i.test(t)) return 'Servicios';

    // Salud / Educación
    if (/\b(farmacia|medic|doctor|consulta|hospital|clinica|cl[ií]nica|seguro)\b/i.test(t)) return 'Salud';
    if (/\b(colegio|universidad|mensualidad|libro|fotocopia|curso|clase)\b/i.test(t)) return 'Educación';

    // Entretenimiento / Compras
    if (/\b(cine|netflix|spotify|juego|bar|discoteca|salida)\b/i.test(t)) return 'Entretenimiento';
    if (/\b(ropa|zapato|tenis|celular|laptop|audifono|aud[ií]fono|compra|compras)\b/i.test(t)) return 'Compras';

    // Transferencias (si el concepto lo deja claro)
    if (/\b(transfer|transferencia|qr|pago)\b/i.test(t)) return 'Transferencias';

    return null;
};

// Función "fire-and-forget" para no detener el Loop de NodeJS mientras la IA clasifica
const categorizarMovimiento = async (concepto, movimientoId) => {
    try {
        const conceptoNorm = normalizarTexto(concepto);

        // 1) Si el concepto es evidente, NO dependemos de la IA (heurística gana siempre).
        const catHeur = categoriaPorHeuristica(conceptoNorm);
        if (catHeur) {
            const categoriaId = await Categoria.obtenerIdPorNombre(catHeur);
            if (categoriaId) {
                await Movimiento.actualizarCategoria(movimientoId, categoriaId);
                return categoriaId;
            }
        }

        // 2) Si no hay concepto, la IA no puede clasificar bien.
        if (!conceptoNorm) return null;

        const response = await fetch("http://localhost:8000/categorizar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ descripcion: conceptoNorm })
        });
        
        if (!response.ok) {
            const body = await response.text().catch(() => "");
            console.warn("[IA] Respuesta no OK al categorizar:", response.status, body);
            return null;
        }

        const data = await response.json();

        // Si la IA se equivoca, igual damos chance a heurística (segunda pasada).
        // En la práctica: “hamburguesa” debe ser Alimentación aunque IA diga Compras/Otros.
        const heurPost = categoriaPorHeuristica(conceptoNorm);
        const categoriaNombreFinal = heurPost || data?.categoria;

        const categoriaId = await Categoria.obtenerIdPorNombre(categoriaNombreFinal);
        if (!categoriaId) {
            console.warn("[IA] Categoría no encontrada en BD, no se pudo actualizar:", categoriaNombreFinal, "movimientoId:", movimientoId);
            return null;
        }

        await Movimiento.actualizarCategoria(movimientoId, categoriaId);
        return categoriaId;
    } catch (error) {
        console.warn("[IA] Fallo no crítico conectando a microservicio de python:", error.message);
    }
    return null;
};

const MovimientoController = {
    // Crear un movimiento (INGRESO, EGRESO, o TRANSFERENCIA)
    crear: async (req, res) => {
        try {
            let { cuenta_id, monto, tipo, concepto, tipo_transaccion, cuenta_destino_id, numero_cuenta_destino } = req.body;
            const usuario_id = req.usuario.id;

            // ... previous logic to validate source account ...
            const cuentasUsuario = await Cuenta.listarPorUsuarioId(usuario_id);
            const cuentaPropia = cuentasUsuario.find(c => c.id === parseInt(cuenta_id));

            if (!cuentaPropia) {
                return res.status(403).json({ mensaje: "No tienes permiso sobre esta cuenta o la cuenta no existe" });
            }

            if (tipo === 'TRANSFERENCIA') {
                // Resolver ID de destino por número si se proporciona
                if (!cuenta_destino_id && numero_cuenta_destino) {
                    const cuentaDestino = await Cuenta.buscarPorNumero(numero_cuenta_destino);
                    if (cuentaDestino) {
                        cuenta_destino_id = cuentaDestino.id;
                    } else {
                        return res.status(404).json({ mensaje: "Cuenta de destino no encontrada" });
                    }
                }

                if (!cuenta_destino_id) {
                    return res.status(400).json({ mensaje: "Se requiere una cuenta de destino para transferencias" });
                }
                
                // Validar que no sea la misma cuenta
                if (parseInt(cuenta_id) === parseInt(cuenta_destino_id)) {
                    return res.status(400).json({ mensaje: "No puedes transferir a la misma cuenta" });
                }

                // Validar saldo suficiente
                if (parseFloat(cuentaPropia.saldo) < parseFloat(monto)) {
                    return res.status(400).json({ mensaje: "Saldo insuficiente" });
                }

                const result = await Movimiento.registrarTransferencia({
                    cuenta_id,
                    cuenta_destino_id,
                    monto,
                    concepto,
                    tipo_transaccion: tipo_transaccion || 'MOVIMIENTO_BANCARIO',
                    numero_transaccion: `TRX-${Date.now()}`
                });

                // 1) Dejar SIEMPRE un valor no-NULL al instante.
                //    Así, si la IA tarda o falla, no se queda categoria_id en NULL en la tabla.
                const otrosId = await Categoria.obtenerIdPorNombre('Otros').catch(() => null);
                if (otrosId) {
                    await Promise.all([
                        Movimiento.actualizarCategoria(result.egresoId, otrosId),
                        Movimiento.actualizarCategoria(result.ingresoId, otrosId)
                    ]).catch(console.error);
                }

                // 2) SIN ESPERAR LA CATEGORIZACIÓN: actualizar en segundo plano (heurística/IA).
                Promise.all([
                    categorizarMovimiento(result.conceptoEgreso, result.egresoId),
                    categorizarMovimiento(result.conceptoIngreso, result.ingresoId)
                ]).catch(console.error);

                return res.json({ mensaje: "Transferencia realizada con éxito", nuevoSaldo: result.nuevoSaldoOrigen });
            }

            // Para INGRESO o EGRESO simple
            if (tipo === 'EGRESO' && parseFloat(cuentaPropia.saldo) < parseFloat(monto)) {
                return res.status(400).json({ mensaje: "Saldo insuficiente para el retiro" });
            }

            const movimiento = await Movimiento.registrar({
                cuenta_id,
                monto,
                tipo,
                concepto: concepto || (tipo === 'INGRESO' ? 'Depósito' : 'Retiro'),
                tipo_transaccion: tipo_transaccion || 'MOVIMIENTO_BANCARIO',
                usuario_accion_id: usuario_id,
                numero_transaccion: `TRX-${Date.now()}`
            });

            // Actualizar saldo en la cuenta
            const nuevoSaldo = tipo === 'INGRESO' ? (parseFloat(cuentaPropia.saldo) + parseFloat(monto)) : (parseFloat(cuentaPropia.saldo) - parseFloat(monto));
            await Cuenta.actualizarSaldo(cuentaPropia.id, nuevoSaldo);

            // Dejar SIEMPRE un valor no-NULL al instante (mínimo: Otros).
            const otrosId = await Categoria.obtenerIdPorNombre('Otros').catch(() => null);
            if (otrosId) {
                await Movimiento.actualizarCategoria(movimiento.id, otrosId).catch(console.error);
            }

            // SIN ESPERAR LA CATEGORIZACIÓN: Actualizamos el registro asíncronamente
            categorizarMovimiento(movimiento.concepto, movimiento.id).catch(console.error);

            res.status(201).json({ mensaje: "Movimiento registrado", movimiento, nuevoSaldo });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al procesar movimiento" });
        }
    },

    // Listar movimientos del usuario
    listar: async (req, res) => {
        try {
            const usuario_id = req.usuario.id;
            const movimientos = await Movimiento.obtenerPorUsuarioId(usuario_id);
            res.json(movimientos);
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al listar movimientos" });
        }
    }
};

module.exports = MovimientoController;
