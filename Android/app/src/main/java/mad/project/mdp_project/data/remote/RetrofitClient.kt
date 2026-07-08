package mad.project.mdp_project.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    // Ganti dengan IP server kamu:
    // - Emulator Android Studio: "http://10.0.2.2:3000/"
    // - Device fisik (WiFi): "http://<IP_KOMPUTER>:3000/"
    private const val BASE_URL = "https://mdp-project-production.up.railway.app/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
