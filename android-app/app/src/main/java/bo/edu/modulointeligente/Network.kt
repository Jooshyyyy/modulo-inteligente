package bo.edu.modulointeligente

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>
    
    @POST("api/auth/registro")
    suspend fun registrarUsuario(@Body request: RegistroRequest): retrofit2.Response<LoginResponse>
    
    // Rutas protegidas
    @GET("api/cuentas/mis-cuentas")
    suspend fun getCuentas(): Response<List<CuentaResponse>>

    @GET("api/cuentas/mi-saldo/{usuario_id}")
    suspend fun getSaldo(@Path("usuario_id") id: Int): Response<CuentaResponse>

    @GET("api/cuentas/mis-movimientos/{cuenta_id}")
    suspend fun getMovimientos(@Path("cuenta_id") id: Int): Response<List<MovimientoResponse>>

    @GET("api/contactos/usuario/{usuario_id}")
    suspend fun getContactos(@Path("usuario_id") id: Int): Response<List<ContactoResponse>>

    @POST("api/contactos")
    suspend fun crearContacto(@Body request: ContactoRequest): Response<Any>

    @retrofit2.http.PUT("api/contactos/{id}")
    suspend fun actualizarContacto(@Path("id") id: Int, @Body request: ContactoRequest): Response<Any>

    @retrofit2.http.DELETE("api/contactos/{id}")
    suspend fun eliminarContacto(@Path("id") id: Int): Response<Any>

    @POST("api/movimientos/crear")
    suspend fun transferir(@Body request: TransferRequest): Response<Any>

    @GET("api/usuarios/{id}")
    suspend fun getPerfil(@Path("id") id: Int): Response<PerfilResponse>

    @retrofit2.http.PUT("api/usuarios/{id}")
    suspend fun actualizarPerfil(@Path("id") id: Int, @Body request: ProfileRequest): Response<Any>

    @GET("api/predicciones/dia")
    suspend fun getPrediccionDia(@retrofit2.http.Query("fecha") fecha: String): Response<PrediccionResponse>

    @GET("api/predicciones/semanal")
    suspend fun getPrediccionSemanal(
        @retrofit2.http.Query("fecha_inicio") fechaInicio: String
    ): Response<PrediccionSemanalResponse>

    @GET("api/predicciones/mensual")
    suspend fun getPrediccionMensual(
        @retrofit2.http.Query("mes") mes: String
    ): Response<PrediccionMensualResponse>

    @GET("api/metas/activa")
    suspend fun getMetaActiva(): Response<MetaActivaResponse>

    @GET("api/metas/ia-coach")
    suspend fun getIaCoach(
        @retrofit2.http.Query("mes") mes: String
    ): Response<IaCoachResponse>

    @POST("api/metas")
    suspend fun crearMeta(@Body request: CrearMetaRequest): Response<CrearMetaApiResponse>

    @retrofit2.http.PUT("api/metas/{id}/progreso")
    suspend fun actualizarProgresoMeta(
        @Path("id") id: Int,
        @Body request: ActualizarMetaProgresoRequest
    ): Response<CrearMetaApiResponse>

    @retrofit2.http.PUT("api/metas/{id}/pausar")
    suspend fun pausarMeta(@Path("id") id: Int): Response<Map<String, String>>
}

object RetrofitClient {
<<<<<<< HEAD
    private const val BASE_URL = "http://192.168.0.7:3000/"
=======
    private const val BASE_URL = "http://192.168.0.11:3000/"
>>>>>>> 799f6e6ae5ec037d2af0f8d4ba89853c260b5b0b
    
    var authToken: String? = null

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}