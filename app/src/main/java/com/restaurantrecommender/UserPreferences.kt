package com.restaurantrecommender

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class UserPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_USER_ID  = "user_id"
        private const val KEY_CITY = "city"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_TOP_RESTAURANTS = "top_restaurants"
        private const val KEY_CLICKED_RESTAURANTS = "clicked_restaurants"
        private const val MAX_ITEMS = 100
    }

    var clickedRestaurants: List<String>
        get() {
            val json = sharedPreferences.getString(KEY_CLICKED_RESTAURANTS, null)
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
        set(value) {
            val updatedList = manageListSize(value)
            val json = gson.toJson(updatedList)
            sharedPreferences.edit().putString(KEY_CLICKED_RESTAURANTS, json).apply()
        }

    // Function to add a restaurant to the list of clicked restaurants
    fun addClickedRestaurant(restaurantName: String) {
        val currentList = clickedRestaurants.toMutableList()
        currentList.add(restaurantName)
        clickedRestaurants = currentList
    }

    var userId: String?
        get() {
            // If the user ID doesn't exist, generate one and save it
            var id = sharedPreferences.getString(KEY_USER_ID, null)
            if (id.isNullOrEmpty()) {
                id = UUID.randomUUID().toString()
                sharedPreferences.edit().putString(KEY_USER_ID, id).apply()
            }
            return id
        }
        set(value) = sharedPreferences.edit().putString(KEY_USER_ID, value).apply()

    var city: String?
        get() = sharedPreferences.getString(KEY_CITY, null)
        set(value) = sharedPreferences.edit().putString(KEY_CITY, value).apply()

    var searchQuery: List<String>
        get() {
            val json = sharedPreferences.getString(KEY_SEARCH_QUERY, null)
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
        set(value) {
            val updatedList = manageListSize(value)
            val json = gson.toJson(updatedList)
            sharedPreferences.edit().putString(KEY_SEARCH_QUERY, json).apply()
        }

    var topRestaurants: List<List<String>>
        get() {
            val json = sharedPreferences.getString(KEY_TOP_RESTAURANTS, null)
            val type = object : TypeToken<List<List<String>>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
        set(value) {
            val updatedList = manageListSize(value)
            val json = gson.toJson(updatedList)
            sharedPreferences.edit().putString(KEY_TOP_RESTAURANTS, json).apply()
        }
    var latitude: String?
        get() = sharedPreferences.getString("latitude", null)
        set(value) = sharedPreferences.edit().putString("latitude", value).apply()

    var longitude: String?
        get() = sharedPreferences.getString("longitude", null)
        set(value) = sharedPreferences.edit().putString("longitude", value).apply()

    private fun <T> manageListSize(list: List<T>): List<T> {
        return if (list.size > MAX_ITEMS) {
            list.takeLast(MAX_ITEMS)
        } else {
            list
        }
    }

    fun addSearchQuery(query: String) {
        val currentList = searchQuery.toMutableList()
        currentList.add(query)
        searchQuery = currentList
    }

    fun addTopRestaurants(restaurants: List<String>) {
        val currentList = topRestaurants.toMutableList()
        currentList.add(restaurants)
        topRestaurants = currentList
    }

}
