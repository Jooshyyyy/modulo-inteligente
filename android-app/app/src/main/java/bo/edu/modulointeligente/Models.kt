package bo.edu.modulointeligente

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
    val nombre: String,
    val apellido: String?,
    val rol: String,
    val email: String?
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
    val fecha: String
)