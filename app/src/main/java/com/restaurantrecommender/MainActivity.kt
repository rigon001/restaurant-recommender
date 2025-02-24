package com.restaurantrecommender

//import androidx.compose.ui.platform.Surface
//import android.os.Bundleimport

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


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

data class RestaurantNew(
    val title: String,
    val url: String,
//    val textSnippet: String,
    val address: String,
    @SerializedName("phone number")
    val phone: String,
    val cuisines: List<String>? = null,
    val meals: List<String>? = null,
    @SerializedName("price range")
    val price: String,
    val features: List<String>? = null,
    @SerializedName("filtered_reviews")
    val reviews: List<String>? = null,
    @SerializedName("geo_lat")
    val latitude: Double? = null,
    @SerializedName("geo_long")
    val longitude: Double? = null,
    val similarity: Double
)

data class RecommendationResponse(
    val recommendations: List<Restaurant>
)

data class RecommendationResponseNew(
    val recommendations: List<RestaurantNew>
)

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // List of cities loaded from CSV file
    private var cities by mutableStateOf<List<String>>(emptyList())
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Destination path in internal storage
        val internalModelDir = File(filesDir, "entity_ruler_patterns")

        // Copy the model if not already copied
        if (!internalModelDir.exists()) {
            internalModelDir.mkdirs()
            copyAssetsToInternalStorage(this, "entity_ruler_patterns", internalModelDir)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the WebView
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
//            loadUrl("file:///android_asset/map.html")
        }
        setupWebView()

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
                        cities = cities,
                        webView = webView
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
                        // Load the HTML file after location is available
                        setupWebView()
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
    private fun setupWebView() {
        val webAppInterface = WebAppInterface(this)
        webView.addJavascriptInterface(webAppInterface, "Android")

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Enable fullscreen functionality
        webView.webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var fullscreenCallback: CustomViewCallback? = null
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                fullscreenCallback = callback

                // Add the view to the activity
                (window.decorView as FrameLayout).addView(view)
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onHideCustomView() {
                // Remove the custom view
                customView?.let {
                    (window.decorView as FrameLayout).removeView(it)
                    customView = null
                    fullscreenCallback = null
                }
                // Revert UI flags
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                Log.d("WebView", "Console Message: ${message?.message()}")
                return super.onConsoleMessage(message)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false // Ensure navigation stays within the WebView
            }
        }

        webView.loadUrl("file:///android_asset/map.html")
    }

}

@Composable
fun ContentWithTitle(modifier: Modifier = Modifier, resources: Resources, cities: List<String>, webView: WebView) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    var userId by remember { mutableStateOf(userPreferences.userId ?: "") }
    // State for search query
    var searchQuery by remember { mutableStateOf("") }

    // State for selected radio button
    var selectedOption by remember { mutableStateOf("Location Based Search") }

    // State for search results
    var searchResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var searchResultsNew by remember { mutableStateOf<List<RestaurantNew>>(emptyList()) }


    // State for loading status
    var isLoading by remember { mutableStateOf(false) }

    // State for dropdown menu
    var expanded by remember { mutableStateOf(false) }
    var selectedCity by remember { mutableStateOf("Ljubljana") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    // State for search submission
    var hasSearched by remember { mutableStateOf(false) }
    // State to control WebView visibility
    var webViewVisible by remember { mutableStateOf(false) }


    // Function to make API call
    fun searchRestaurants(selectedOption: String, query: String, city: String? = null, userPreferences: UserPreferences, context: Context) {
        isLoading = true
        hasSearched = true

        // Show the map WebView after search
        webViewVisible = true
        // Save search query
        userPreferences.addSearchQuery(query)

        // Initialize Python if not already started
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        var enhancedQuery = ""
        val entityRulerPath = File(context.filesDir, "entity_ruler_patterns").absolutePath
        Log.d("PYTHON","entityRulerPath: $entityRulerPath")


        try {
            Log.d("PYTHON","Spacy creation.")
            val python = Python.getInstance()
            val pyModule = python.getModule("spacy_handler") // Use your Python module name
//            pyModule.callAttr("load_entity_ruler") // Call the function

            val entities = pyModule.callAttr("process_sentence",
                query,
                entityRulerPath).asList()
            for (entity in entities) {
                val entityText = entity.asList()[0].toString()
                val entityLabel = entity.asList()[1].toString()
                Log.d("PYTHON","Entity: $entityText, Label: $entityLabel")
                // Save the entity to the appropriate user preference based on the label
                when (entityLabel) {
                    "CUISINE" -> {
                        userPreferences.addUserStyle(entityText)
                    }
                    "PRICE" -> {
                        userPreferences.addUserPrice(entityText)
                    }
                    "MEALS" -> {
                        userPreferences.addUserMeals(entityText)
                    }
                    "FEATURE" -> {
                        userPreferences.addUserFeatures(entityText)
                    }
                    "RATING" -> {
                        userPreferences.addUserRatings(entityText)
                    }
                    else -> {
                        Log.d("PYTHON","Unrecognized label: $entityLabel")
                    }
                }
            }
            Log.d("PYTHON","spacy_handler creation triggered successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("PYTHON","Error triggering EntityRuler creation: ${e.message}")
        }

        // Run enhance_query Python script
        try {

            // Convert UserPreferences to JSON-like format
            val userPrefsMap = mapOf(
                "user_styles" to userPreferences.userStyles,
                "user_prices" to userPreferences.userPrice,
                "user_meals" to userPreferences.userMeals,
                "user_features" to userPreferences.userFeatures,
                "user_ratings" to userPreferences.userRatings,
                "restaurant_styles" to userPreferences.restaurantStyles,
                "restaurant_prices" to userPreferences.restaurantPrices,
                "restaurant_meals" to userPreferences.restaurantMeals,
                "restaurant_features" to userPreferences.restaurantFeatures,
                "restaurant_ratings" to userPreferences.restaurantRatings
            )

            // Convert the map to a JSON string
            val userPrefsJson = Gson().toJson(userPrefsMap)

            val py = Python.getInstance()
            val module = py.getModule("complement_query") // Python script name without .py

            enhancedQuery = module.callAttr(
                "enhance_query", // Python function to call
                query,
                userPrefsJson, // Pass the path to the user_prefs.xml
                entityRulerPath // EntityRuler path
            ).toString()

            // Display the enhanced query
            Log.d("PYTHON","Enhanced Query: $enhancedQuery")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("PYTHON","Error triggering query enhancement: ${e.message}")
        }

        // Prepare the JSON payload
        val jsonPayload = if (selectedOption == "Similar Restaurants Search") {
            mapOf("input" to enhancedQuery, "city" to (city ?: ""))
        } else {
            mapOf("input" to enhancedQuery)
        }

        Log.d("Search", "Making API call jsonpayload: $jsonPayload")

        val call = when (selectedOption) {
            "Location Based Search" -> RetrofitInstance.api.getOtherRestaurants(jsonPayload)
            "Similar Restaurants Search" -> RetrofitInstance.api.getRestaurants(jsonPayload)
            "New API Search" -> RetrofitInstance.api.getRestaurantsNew(jsonPayload) // ✅ New API Call
            else -> throw IllegalArgumentException("Unsupported model: $selectedOption")
        }

        Log.d("Search", "Making API call with model: $selectedOption, query: $enhancedQuery")
        Log.d("Search", "Making API call jsonpayload: $jsonPayload")

        if (selectedOption == "New API Search") {
            (call as Call<RecommendationResponseNew>).enqueue(object : Callback<RecommendationResponseNew> {
                override fun onResponse(call: Call<RecommendationResponseNew>, response: Response<RecommendationResponseNew>) {
                    isLoading = false
                    if (response.isSuccessful) {
                        searchResultsNew = response.body()?.recommendations ?: emptyList()
                        // ✅ Add this line to load markers on the map
                        loadMapWithMarkers(webView, searchResultsNew)
                    } else {
                        Log.d("Search", "New API call failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<RecommendationResponseNew>, t: Throwable) {
                    isLoading = false
                    Log.e("Search", "New API call error: ${t.message}")
                }
            })
        } else {
            (call as Call<RecommendationResponse>).enqueue(object : Callback<RecommendationResponse> {
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
                            Log.d("Search", "API call successful. " +
                                    "Received ${searchResults.size} results.")
                            loadMapWithMarkers(webView, searchResults)
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
                    Log.e("Search", "Old API call error: ${t.message}")
                }
            })
        }

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

            // Map Section
            if (webViewVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                ) {
                    LocalWebView(webView = webView)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Map Section ended

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedOption == "New API Search",
                        onClick = { selectedOption = "New API Search" }
                    )
                    Text(text = "New API Search",
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
            Spacer(modifier = Modifier.height(10.dp)) // Add some space after radio buttons
            // Search box and button in one row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                TextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        searchQuery = newQuery
                        hasSearched=false},
                    placeholder = { Text("Search for a restaurant") },
                    modifier = Modifier
                        .weight(1f) // Ensures the TextField takes available space
                        .padding(end = 8.dp), // Adds spacing between TextField and Button,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { // No need for explicit cast
                            coroutineScope.launch {
                                searchRestaurants(
                                    selectedOption,
                                    searchQuery,
                                    if (selectedOption == "Similar Restaurants Search") selectedCity else null,
                                    userPreferences,
                                    context
                                )
                                delay(500) // Delay for 0.5 seconds (500 milliseconds)
                                keyboardController?.hide()
                            }
                        }
                    )
                )
                IconButton(
                    onClick = {
                        userPreferences.userId = userId
                        userPreferences.city = selectedCity
                        coroutineScope.launch {
                            searchRestaurants(selectedOption, searchQuery,
                                if (selectedOption == "Similar Restaurants Search") selectedCity
                                else null, userPreferences, context)
                            delay(500) // Delay for 0.5 seconds (500 milliseconds)
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search, // Default search icon from Material Design
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add space after button
            // Show filtered restaurants only if searchQuery is not empty
            if (isLoading) {
                Text(text = "Loading...")
            } else if (searchResults.isEmpty() && searchResultsNew.isEmpty() && hasSearched) {
                if (searchQuery.isNotEmpty()) {
                    Text(text = "No restaurants found.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn {
                    // Case 1: Display results from the new API (if selected)
                    if (selectedOption == "New API Search" && searchResultsNew.isNotEmpty()) {
                        items(searchResultsNew) { restaurantNew ->
                            RestaurantNewCard(restaurantNew = restaurantNew, webView = webView)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Case 2: Display results from the existing API (if selected)
                    if (selectedOption != "New API Search" && searchResults.isNotEmpty()) {
                        items(searchResults) { restaurant ->
                            RestaurantCard(restaurant = restaurant, webView = webView)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant,  webView: WebView) {
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)  // Access UserPreferences here

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Track the clicked restaurant
                userPreferences.addClickedRestaurant(restaurant.name)
                userPreferences.addRestaurantStyle(restaurant.style)
                userPreferences.addRestaurantPrice(restaurant.price)


                // Trigger the marker click in the WebView
                webView.evaluateJavascript(
                    """
                (function() {
                    if (typeof markersLayerGroup === 'undefined') {
                        console.log('markersLayerGroup is undefined.');
                        return null;
                    }
            
                    var markers = markersLayerGroup.getLayers();
                    console.log('Markers count:', markers.length);
            
                    for (var i = 0; i < markers.length; i++) {
                        console.log('Marker popup content:', markers[i].getPopup().getContent());
                        if (markers[i].getPopup().getContent().includes("${restaurant.address}")) {
                            console.log('Found matching marker for address:', "${restaurant.address}");
                            
                            // Smoothly pan to the marker
                            map.flyTo(markers[i].getLatLng(), 16, {
                                animate: true,
                                duration: 1.5
                            });
            
                            // Open the marker's popup
                            setTimeout(() => {
                                markers[i].openPopup();
                            }, 1600); // Delay matches the duration of flyTo (1.6 seconds = 1600 ms)
                            return 'success';
                        }
                    }
            
                    console.log('No marker found for address:', "${restaurant.address}");
                    return null;
                })();
                """.trimIndent()
                ) { result ->
                    Log.d("WebView", "Marker click triggered: $result")
                }
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

@Composable
fun RestaurantNewCard(restaurantNew: RestaurantNew, webView: WebView) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)  // Access UserPreferences here

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Track the clicked restaurant
                userPreferences.addClickedRestaurant(restaurantNew.title)

                // Trigger marker click in WebView
                webView.evaluateJavascript(
                    """
                (function() {
                    if (typeof markersLayerGroup === 'undefined') {
                        console.log('markersLayerGroup is undefined.');
                        return null;
                    }
            
                    var markers = markersLayerGroup.getLayers();
                    console.log('Markers count:', markers.length);
            
                    for (var i = 0; i < markers.length; i++) {
                        console.log('Marker popup content:', markers[i].getPopup().getContent());
                        if (markers[i].getPopup().getContent().includes("${restaurantNew.address}")) {
                            console.log('Found matching marker for address:', "${restaurantNew.address}");
                            
                            // Smoothly pan to the marker
                            map.flyTo(markers[i].getLatLng(), 16, {
                                animate: true,
                                duration: 1.5
                            });
            
                            // Open the marker's popup
                            setTimeout(() => {
                                markers[i].openPopup();
                            }, 1600); // Delay matches the duration of flyTo (1.6 seconds = 1600 ms)
                            return 'success';
                        }
                    }
            
                    console.log('No marker found for address:', "${restaurantNew.address}");
                    return null;
                })();
                """.trimIndent()
                ) { result ->
                    Log.d("WebView", "Marker click triggered: $result")
                }
            },
        color = MaterialTheme.colorScheme.surface
    ){
        Card(
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = restaurantNew.title,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = "Snippet: ${restaurantNew.textSnippet}",
//                style = TextStyle(fontSize = 16.sp)
//            )
                Spacer(modifier = Modifier.height(4.dp))
                // Clickable URL
                ClickableText(
                    text = AnnotatedString(restaurantNew.url),
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Blue, // Makes it look like a link
                        textDecoration = TextDecoration.Underline
                    ),
                    onClick = {
                        uriHandler.openUri(restaurantNew.url) // Open URL in a browser
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Address: ${restaurantNew.address}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone: ${restaurantNew.phone}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                restaurantNew.cuisines?.let { cuisines ->
                    Text(
                        text = "Cuisines:",
                        style = TextStyle(fontSize = 16.sp)
                    )
                    cuisines.forEach { cuisine ->
                        Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                            Text(
                                text = "•",
                                style = TextStyle(fontSize = 14.sp),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = cuisine,
                                style = TextStyle(fontSize = 14.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                restaurantNew.meals?.let { meals ->
                    Text(
                        text = "Meals:",
                        style = TextStyle(fontSize = 16.sp)
                    )
                    meals.forEach { meal ->
                        Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                            Text(
                                text = "•",
                                style = TextStyle(fontSize = 14.sp),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = meal,
                                style = TextStyle(fontSize = 14.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Price: ${restaurantNew.price}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                restaurantNew.features?.let { features ->
                    Text(
                        text = "Features:",
                        style = TextStyle(fontSize = 16.sp)
                    )
                    features.forEach { feature ->
                        Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                            Text(
                                text = "•",
                                style = TextStyle(fontSize = 14.sp),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = feature,
                                style = TextStyle(fontSize = 14.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                restaurantNew.reviews?.let { reviews ->
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Latitude: ${restaurantNew.latitude}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Longitude: ${restaurantNew.longitude}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Similarity: ${restaurantNew.similarity}",
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

    }
}

fun loadMapWithMarkers(webView: WebView, restaurants: List<Any>) {
    val simplifiedRestaurantsOld = mutableListOf<Map<String, String>>() // For old API
    val simplifiedRestaurantsNew = mutableListOf<Map<String, Any>>() // For new API with geo-coordinates

    // Separate lists based on type
    restaurants.forEach { restaurant ->
        when (restaurant) {
            is Restaurant -> simplifiedRestaurantsOld.add(
                mapOf(
                    "name" to restaurant.name,
                    "address" to restaurant.address
                )
            )
            is RestaurantNew -> simplifiedRestaurantsNew.add(
                mapOf(
                    "name" to restaurant.title,
                    "address" to restaurant.address,
                    "latitude" to (restaurant.latitude ?: 0.0),  // Use 0.0 if null
                    "longitude" to (restaurant.longitude ?: 0.0) // Use 0.0 if null
                )
            )
        }
    }

    // Convert lists to JSON
    val restaurantsJsonOld = Gson().toJson(simplifiedRestaurantsOld.take(10)) // Take the first 10 restaurants
    val restaurantsJsonNew = Gson().toJson(simplifiedRestaurantsNew.take(10)) // Take the first 10 restaurants

    // Inject JavaScript functions based on API type
    if (simplifiedRestaurantsOld.isNotEmpty()) {
        webView.evaluateJavascript("addMarkersForOldApi($restaurantsJsonOld);", null)
    }
    if (simplifiedRestaurantsNew.isNotEmpty()) {
        webView.evaluateJavascript("addMarkersWithCoordinates($restaurantsJsonNew);", null)
    }
}


@Composable
fun LocalWebView(webView: WebView) {
    AndroidView(factory = { context ->
        webView.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // Enable JavaScript if needed
            settings.javaScriptEnabled = true

            // Set a WebViewClient to handle loading
            webViewClient = WebViewClient()

            // Load the local HTML file from the assets folder
            loadUrl("file:///android_asset/map.html")
        }
    })
}

fun copyAssetsToInternalStorage(context: Context, assetDir: String, outputDir: File) {
    val assetManager = context.assets
    val files = assetManager.list(assetDir) ?: return

    for (file in files) {
        val assetPath = "$assetDir/$file"
        val outFile = File(outputDir, file)

        if (assetManager.list(assetPath)?.isNotEmpty() == true) {
            // Create directories for subfolders
            outFile.mkdirs()
            copyAssetsToInternalStorage(context, assetPath, outFile)
        } else {
            // Copy file
            assetManager.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
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