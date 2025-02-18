package com.restaurantrecommender.network

import com.restaurantrecommender.Restaurant
import com.restaurantrecommender.RecommendationResponse
import com.restaurantrecommender.RecommendationResponseNew
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RestaurantApiService {
    @POST("recommend") // Adjust endpoint URL as per your server setup
    fun getRestaurants(
//        @Query("model") model: String,
//        @Query("query") query: String
        @Body jsonPayload: Map<String, String>
    ): Call<RecommendationResponse> // Specify the return type as List<Restaurant>

    @POST("recommend2") // Adjust endpoint URL as per your server setup
    fun getOtherRestaurants(
//        @Query("model") model: String,
//        @Query("query") query: String
        @Body jsonPayload: Map<String, String>
    ): Call<RecommendationResponse> // Specify the return type as List<Restaurant>

    @POST("recommend_new") // Adjust endpoint URL as per your server setup
    fun getRestaurantsNew(
//        @Query("model") model: String,
//        @Query("query") query: String
        @Body jsonPayload: Map<String, String>
    ): Call<RecommendationResponseNew>

}