package bo.edu.modulointeligente

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>
    @POST("api/auth/registro")
    suspend fun registrarUsuario(@Body request: RegistroRequest): retrofit2.Response<LoginResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.19:3000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}