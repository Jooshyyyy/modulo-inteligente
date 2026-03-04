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
    val fecha: String
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
    val cuenta_destino_id: Int? = null,
    val tipo_transaccion: String = "MOVIMIENTO_BANCARIO"
)