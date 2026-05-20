package bo.edu.modulointeligente

import com.google.gson.annotations.SerializedName

// Lo que envía
data class LoginRequest(
    val numero_carnet: String,
    val password: String
)

// Lo que recibe
data class LoginResponse(
    val mensaje: String,
    val token: String?,
    val usuario: UsuarioData?
)

data class UsuarioData(
    val id: Int,
    val primer_nombre: String,
    val segundo_nombre: String?,
    val apellido_paterno: String,
    val apellido_materno: String?,
    val nombre: String,
    val apellido: String?,
    val rol: String,
    val email: String?
)

data class PerfilResponse(
    val id: Int,
    val primer_nombre: String,
    val segundo_nombre: String?,
    val apellido_paterno: String,
    val apellido_materno: String?,
    val email: String,
    val numero_carnet: String,
    val telefono: String?,
    val direccion: String?,
    val ocupacion: String?,
    val fecha_nacimiento: String?,
    val nombre_completo: String?
)

data class ProfileRequest(
    val email: String?,
    val password: String?,
    val telefono: String?,
    val ocupacion: String?,
    val direccion: String?,
    // Campos para administrador
    val numero_carnet: String? = null,
    val primer_nombre: String? = null,
    val segundo_nombre: String? = null,
    val apellido_paterno: String? = null,
    val apellido_materno: String? = null,
    val fecha_nacimiento: String? = null
)
data class RegistroRequest(
    val primer_nombre: String,
    val segundo_nombre: String?,
    val apellido_paterno: String,
    val apellido_materno: String?,
    val email: String,
    val password: String,
    val numero_carnet: String,
    val telefono: String?,
    val direccion: String?,
    val ocupacion: String?,
    val fecha_nacimiento: String
)
data class CuentaResponse(
    val id: Int, // El ID de la tabla 'cuentas'
    val numero_cuenta: String,
    val tipo_cuenta: String,
    val saldo: Double,
    val moneda: String
)

data class MovimientoResponse(
    val concepto: String,
    val monto: Double,
    val tipo: String, // 'INGRESO' o 'EGRESO'
    val fecha: String,
    @SerializedName("categoria_id")
    val categoriaId: Int? = null,
    @SerializedName(value = "categoria_nombre", alternate = ["categoria", "nombre_categoria"])
    val categoriaNombre: String? = null
)

data class ContactoResponse(
    val id: Int,
    val usuario_id: Int,
    val nombre: String,
    val alias: String?,
    val cuenta_bancaria: String,
    val nombre_banco: String,
    val moneda: String,
    val estado: String,
    val fecha_registro: String,
    val fecha_edicion: String
)

data class ContactoRequest(
    val usuario_id: Int? = null,
    val nombre: String,
    val alias: String? = null,
    val cuenta_bancaria: String,
    val nombre_banco: String,
    val moneda: String = "BOB",
    val estado: String = "ACTIVO"
)

data class TransferRequest(
    val cuenta_id: Int,
    val monto: Double,
    val tipo: String = "TRANSFERENCIA",
    val numero_cuenta_destino: String?,
    val concepto: String? = null,
    val cuenta_destino_id: Int? = null,
    val tipo_transaccion: String = "MOVIMIENTO_BANCARIO"
)

data class ProbabilidadItem(
    val nombre: String,
    val hora: String,
    val porcentaje: Int,
    val monto: String
)

data class PrediccionResponse(
    val total: String,
    val nivel: String,
    val diferenciaPrevia: String,
    val probabilidades: List<ProbabilidadItem>
)

data class PrediccionSemanalCategoria(
    val categoriaId: Int = 0,
    val categoria: String = "",
    val colorHex: String = "#9E9E9E",
    val monto: Double = 0.0,
    val confianza: Double = 0.0
)

data class PrediccionDetalleDia(
    val fecha: String = "",
    val categoria: String = "",
    val colorHex: String = "#9E9E9E",
    val monto: Double = 0.0,
    val confianza: Double = 0.0
)

data class PrediccionSemanalResponse(
    val fechaInicio: String,
    val total: Double,
    val categorias: List<PrediccionSemanalCategoria>,
    val dias: List<PrediccionDetalleDia>
)

data class PrediccionMensualResponse(
    val mes: String = "",
    val total: Double = 0.0,
    val categorias: List<PrediccionSemanalCategoria> = emptyList(),
    val dias: List<PrediccionDetalleDia> = emptyList()
)

data class CategoriaItem(
    val id: Int,
    val nombre: String,
    val colorHex: String = "#9E9E9E"
)

data class LimiteCategoriaItem(
    @SerializedName("categoria_id") val categoriaId: Int,
    @SerializedName("tope_mensual") val topeMensual: Double
)

data class LimiteCategoriaPlan(
    val categoriaId: Int = 0,
    val categoriaNombre: String = "",
    val topeMensual: Double = 0.0
)

data class PlanPresupuesto(
    val id: Int,
    val titulo: String,
    val tipo: String = "GENERAL",
    val topeMensual: Double? = null,
    val topeTotal: Double = 0.0,
    val limites: List<LimiteCategoriaPlan> = emptyList(),
    val estado: String = ""
)

/** Alias de compatibilidad con respuestas que aún envían `meta`. */
typealias MetaFinanciera = PlanPresupuesto

data class MetaActivaResponse(
    val tieneMeta: Boolean,
    val meta: PlanPresupuesto? = null,
    val plan: PlanPresupuesto? = null
)

data class CrearMetaRequest(
    val titulo: String,
    val tipo: String,
    @SerializedName("tope_mensual") val topeMensual: Double? = null,
    val limites: List<LimiteCategoriaItem>? = null
)

data class CrearMetaApiResponse(
    val mensaje: String,
    val meta: PlanPresupuesto? = null,
    val plan: PlanPresupuesto? = null
)

data class CategoriaEstadoPresupuesto(
    val categoriaId: Int = 0,
    val categoriaNombre: String = "",
    val topeMensual: Double = 0.0,
    val gastoProyectado: Double = 0.0,
    val margen: Double = 0.0,
    val exceso: Double = 0.0,
    val usoPct: Double = 0.0
)

data class EstadoPresupuestoCoach(
    val tope: Double = 0.0,
    val gasto: Double = 0.0,
    val margen: Double = 0.0,
    val exceso: Double = 0.0,
    val ahorroProyectado: Double = 0.0,
    val usoPct: Double = 0.0,
    val categoriasEstado: List<CategoriaEstadoPresupuesto> = emptyList()
)

data class IaCoachIndicador(
    val clave: String,
    val etiqueta: String,
    val valor: String,
    val detalle: String
)

data class IaSugerencia(
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val categoria: String? = null,
    val fecha: String? = null,
    val montoProyectado: Double,
    val montoAhorroSugerido: Double,
    val porcentajeAcercamientoMeta: Double,
    val prioridad: Int
)

data class IaCoachResponse(
    val tieneMeta: Boolean,
    val meta: PlanPresupuesto? = null,
    val plan: PlanPresupuesto? = null,
    val mes: String? = null,
    val gastoProyectadoMes: Double = 0.0,
    val diasConPrediccion: Int = 0,
    val narrativa: String,
    val indicadores: List<IaCoachIndicador> = emptyList(),
    val sugerencias: List<IaSugerencia> = emptyList(),
    val estadoPresupuesto: EstadoPresupuestoCoach? = null
)