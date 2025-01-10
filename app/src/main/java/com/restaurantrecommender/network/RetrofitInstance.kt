package com.restaurantrecommender.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "http://164.8.22.203:5000/"
    private const val API_KEY = "d89b5fbc-b80d-4c18-bd0e-b2444020db53"  // Add your API key here

    // Create an Interceptor to add the API key to the headers
    private val apiKeyInterceptor = Interceptor { chain ->
        val request: Request = chain.request().newBuilder()
            .addHeader("x-api-key", "$API_KEY")  // Add your API key in the Authorization header
            .build()
        chain.proceed(request)
    }

    // Create an OkHttpClient and attach the interceptor
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .build()

    // Retrofit instance with the custom OkHttpClient
    val api: RestaurantApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)  // Attach the OkHttpClient
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RestaurantApiService::class.java)
    }
}
