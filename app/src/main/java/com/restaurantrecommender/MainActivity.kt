package com.restaurantrecommender

//import androidx.compose.ui.platform.Surface
//import android.os.Bundleimport

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restaurantrecommender.network.RetrofitInstance
import com.restaurantrecommender.ui.theme.RestaurantRecommenderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


// Define a data class for Restaurant
data class Restaurant(
    val name: String,
    val city: String,
    val country: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val price: String,
    val style: String,
    val reviews: List<String>? = null,
    val similarity: Double
)

data class RecommendationResponse(
    val recommendations: List<Restaurant>
)

class MainActivity : ComponentActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // List of cities loaded from CSV file
    private var cities by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        enableEdgeToEdge()
        setContent {
            RestaurantRecommenderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val resources = resources
                    // Function to load cities from CSV file
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            cities = readCitiesFromCsv(resources)
                        }
                    }
                    ContentWithTitle(
                        modifier = Modifier.padding(innerPadding),
                        resources = resources,
                        cities = cities
                    )
                }
            }
        }
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, fetch location
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show an explanation to the user as to why the permission is needed
                Log.d("Location", "Show rationale for location permission")
            }
            else -> {
                // Directly request for permission
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, fetch the location
            getCurrentLocation()
        } else {
            Log.d("Location", "Permission denied")
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Log.d("Location", "Current location: Latitude: $latitude, Longitude: $longitude")

                        // Save location to user preferences
                        val userPreferences = UserPreferences(this)
                        userPreferences.latitude = latitude.toString()
                        userPreferences.longitude = longitude.toString()
                    } else {
                        Log.d("Location", "Location is null")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Location", "Failed to get location: ${exception.message}")
                }
        } else {
            Log.d("Location", "Location permission not granted")
        }
    }

}

@Composable
fun ContentWithTitle(modifier: Modifier = Modifier, resources: Resources, cities: List<String>) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    var userId by remember { mutableStateOf(userPreferences.userId ?: "") }
    // State for search query
    var searchQuery by remember { mutableStateOf("") }

    // State for selected radio button
    var selectedOption by remember { mutableStateOf("Location Based Search") }

    // State for search results
    var searchResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }

    // State for loading status
    var isLoading by remember { mutableStateOf(false) }

    // State for dropdown menu
    var expanded by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf("Ljubljana") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    // State for search submission
    var hasSearched by remember { mutableStateOf(false) }

    // Function to make API call
    fun searchRestaurants(selectedOption: String, query: String, city: String? = null) {
        isLoading = true
        hasSearched = true

        // Prepare the JSON payload
        val jsonPayload = if (selectedOption == "Similar Restaurants Search") {
            mapOf("input" to query, "city" to (city ?: ""))
        } else {
            mapOf("input" to query)
        }
        Log.d("Search", "Making API call jsonpayload: $jsonPayload")

        val call = when (selectedOption) {
            "Location Based Search" -> RetrofitInstance.api.getOtherRestaurants(jsonPayload)
            "Similar Restaurants Search" -> RetrofitInstance.api.getRestaurants(jsonPayload)
            else -> throw IllegalArgumentException("Unsupported model: $selectedOption")
        }

        Log.d("Search", "Making API call with model: $selectedOption, query: $searchQuery")
        Log.d("Search", "Making API call jsonpayload: $jsonPayload")
        call.enqueue(object : Callback<RecommendationResponse> {
            override fun onResponse(call: Call<RecommendationResponse>, response: Response<RecommendationResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    val recommendationResponse = response.body()
                    val restaurants = recommendationResponse?.recommendations ?: emptyList()
                    if (restaurants.isEmpty()) {
                        // Handle case where no restaurants are found
                        searchResults = emptyList()
                        Log.d("Search", "No restaurants found.")
                    } else {
                        searchResults = restaurants
                        Log.d("Search", "API call successful. Received ${searchResults.size} results.")

                        // Save search query and top 5 restaurant names
                        userPreferences.addSearchQuery(query)
                        val topRestaurants = restaurants.take(5).map { it.name }
                        userPreferences.addTopRestaurants(topRestaurants)
                    }
                } else {
                    Log.d("Search", "API call unsuccessful. Status code: ${response.code()}")
                    // Handle unsuccessful response
                }
            }

            override fun onFailure(call: Call<RecommendationResponse>, t: Throwable) {
                isLoading = false
                Log.d("Search", "API call failed with exception: ${t.message}")
                // Handle failure
            }
        })
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black//MaterialTheme.colorScheme.onTertiaryContainer //Color.Black
    ) {
        Column(modifier = modifier.padding(24.dp)) {
            Text(
                text = "Restaurant Recommender",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(30.dp)) // Add some space between title and greeting

            // OpenMaps integration start
//            Box(modifier = Modifier.weight(1f)) {
//                if (searchResults.isNotEmpty()) {
//                    OpenStreetMapView(restaurants = searchResults)
//                } else {
//                    Text(
//                        "Explore restaurants on the map!",
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        style = TextStyle(fontSize = 16.sp)
//                    )
//                }
//            }
            // Map Section
            Box(
                modifier = Modifier
//                    .fillMaxWidth()
                    .weight(0.3f) // Proportional height: 30% of the screen height
            ) {
                OpenStreetMapView(restaurants = searchResults)
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Map integration ended
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedOption == "Location Based Search",
                        onClick = { selectedOption = "Location Based Search" }
                    )
                    Text(text = "Location Based Search",
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedOption == "Similar Restaurants Search",
                        onClick = { selectedOption = "Similar Restaurants Search" }
                    )
                    Text(text = "Similar Restaurants Search",
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
                if (selectedOption == "Similar Restaurants Search") {
                    Button(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(text = selectedCity)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedCity = city
                                    expanded = false
                                },
                                text = { Text(text = city) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add some space after radio buttons
            // Search box

            TextField(
                value = searchQuery,
                onValueChange = { newQuery ->
                    searchQuery = newQuery
                    hasSearched=false},
                placeholder = { Text("Search for a restaurant") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { // No need for explicit cast
                        coroutineScope.launch {
                            searchRestaurants(
                                selectedOption,
                                searchQuery,
                                if (selectedOption == "Similar Restaurants Search") selectedCity else null
                            )
                            delay(500) // Delay for 0.5 seconds (500 milliseconds)
                            keyboardController?.hide()
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.height(16.dp)) // Add space after search box
            Button(
                onClick = {
                    userPreferences.userId = userId
                    userPreferences.city = selectedCity
                    coroutineScope.launch {
                        searchRestaurants(selectedOption, searchQuery,
                            if (selectedOption == "Similar Restaurants Search") selectedCity
                            else null)
                        delay(500) // Delay for 0.5 seconds (500 milliseconds)
                        keyboardController?.hide()
                    }
                          },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = "Search")
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add space after button
            // Show filtered restaurants only if searchQuery is not empty
            searchResults.let { results ->
                if (isLoading) {
                    Text(text = "Loading...")
                } else if (results.isEmpty() && hasSearched) {
                    // Show results only if searchQuery is not empty
                    if (searchQuery.isNotEmpty()) {
                        Text(text = "No restaurants found.",
                            color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    LazyColumn {
                        items(results) { restaurant ->
                            RestaurantCard(restaurant = restaurant)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant) {
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)  // Access UserPreferences here

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Track the clicked restaurant
                userPreferences.addClickedRestaurant(restaurant.name)

                // GoogleMaps implementation
                // Create a Uri from an intent string. Use the result to create an Intent.
//                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(restaurant.address)}")
//
//                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
//                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
//
//                // Make the Intent explicit by setting the Google Maps package
//                mapIntent.setPackage("com.google.android.apps.maps")
//
//                // Attempt to start an activity that can handle the Intent
//                if (mapIntent.resolveActivity(context.packageManager) != null) {
//                    context.startActivity(mapIntent)
//                }
                // OpenMaps implementation
                val intent = Intent(context, OpenMapActivity::class.java).apply {
                    putExtra("name", restaurant.name)
                    putExtra("address", restaurant.address)
                }
                context.startActivity(intent)
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
//            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = restaurant.name,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "City: ${restaurant.city}, Country: ${restaurant.country}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Address: ${restaurant.address}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Price: ${restaurant.price}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Style: ${restaurant.style}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Conditional display of reviews section
                restaurant.reviews?.let { reviews ->
                    Text(
                        text = "Reviews:",
                        style = TextStyle(fontSize = 16.sp)
                    )
                    reviews.forEach { review ->
                        Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                            Text(
                                text = "•",
                                style = TextStyle(fontSize = 14.sp),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = review,
                                style = TextStyle(fontSize = 14.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                Text(
                    text = "Similarity: ${restaurant.similarity}",
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }
    }
}

fun truncateReviews(reviews: String, maxLength: Int = 400): String {
    return if (reviews.length > maxLength) {
        "${reviews.substring(0, maxLength)}..."
    } else {
        reviews
    }
}

@Preview(showBackground = true)
@Composable
fun ContentWithTitlePreview() {
    val resources = Resources.getSystem()
    val cities = remember { readCitiesFromCsv(resources) }
    RestaurantRecommenderTheme {
        ContentWithTitle(modifier = Modifier, resources = resources, cities = cities)
    }
}

@Composable
fun OpenStreetMapView(restaurants: List<Restaurant>) {
    AndroidView(
        factory = { context ->
            // Initialize the MapView
            val mapView = org.osmdroid.views.MapView(context).apply {
                setMultiTouchControls(true) // Enable gestures
                controller.setZoom(12.0)   // Default zoom level
            }

            // Set initial map position (default location)
            val defaultPoint = org.osmdroid.util.GeoPoint(37.7749, -122.4194) // Example: San Francisco
            mapView.controller.setCenter(defaultPoint)

            // Add restaurant markers if available
            if (restaurants.isNotEmpty()) {
                restaurants.forEach { restaurant ->
                    val marker = org.osmdroid.views.overlay.Marker(mapView).apply {
                        position = org.osmdroid.util.GeoPoint(restaurant.latitude, restaurant.longitude)
                        title = restaurant.name
                        snippet = "${restaurant.address}, ${restaurant.city}, ${restaurant.country}"
                        setOnMarkerClickListener { _, _ ->
                            Log.d("OpenStreetMap", "Clicked on: ${restaurant.name}")
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            } else {
                // Add a default marker to show the default location
                val defaultMarker = org.osmdroid.views.overlay.Marker(mapView).apply {
                    position = defaultPoint
                    title = "Default Location"
                    snippet = "San Francisco, CA"
                }
                mapView.overlays.add(defaultMarker)
            }
            mapView
        },
        modifier = Modifier
//            .fillMaxSize()
            .height(60.dp)
            .padding(8.dp)
    )
}


@Throws(Exception::class)
private fun readCitiesFromCsv(resources: Resources): List<String> {
    val inputStream = resources.openRawResource(R.raw.cities)
    val reader = BufferedReader(InputStreamReader(inputStream))
    val cities = mutableListOf<String>()
    var line: String? = reader.readLine()
    while (line != null) {
        cities.add(line)
        line = reader.readLine()
    }
    reader.close()
    return cities
}