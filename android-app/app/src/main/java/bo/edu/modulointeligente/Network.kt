package bo.edu.modulointeligente

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>
    @POST("api/auth/registro")
    suspend fun registrarUsuario(@Body request: RegistroRequest): retrofit2.Response<LoginResponse>
    @GET("api/cuentas/mi-saldo/{usuario_id}")
    suspend fun getSaldo(@Path("usuario_id") id: Int): retrofit2.Response<CuentaResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.12.184:3000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}