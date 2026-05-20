const Categoria = require('../models/categoria.model');

const listarCategorias = async (req, res) => {
    try {
        const rows = await Categoria.listar();
        res.json(
            rows.map((r) => ({
                id: r.id,
                nombre: r.nombre,
                colorHex: r.color_hex
            }))
        );
    } catch (e) {
        console.error('Listar categorías:', e);
        res.status(500).json({ mensaje: 'Error al listar categorías.' });
    }
};

module.exports = { listarCategorias };
