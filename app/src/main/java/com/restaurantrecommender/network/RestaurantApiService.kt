package com.restaurantrecommender.network

import com.restaurantrecommender.RecommendationResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RestaurantApiService {
    @POST("recommend_new") // Adjust endpoint URL as per your server setup
    fun getRestaurants(
        @Body jsonPayload: Map<String, String>
    ): Call<RecommendationResponse>

}