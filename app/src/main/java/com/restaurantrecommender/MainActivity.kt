package com.restaurantrecommender

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream



data class Restaurant(
//    val title: String,
    val name: String,
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

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

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
        }
        setupWebView()

        enableEdgeToEdge()
        setContent {
            RestaurantRecommenderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val resources = resources
                    ContentWithTitle(
                        modifier = Modifier.padding(innerPadding),
                        resources = resources,
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
fun ContentWithTitle(modifier: Modifier = Modifier, resources: Resources,  webView: WebView) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    var userId by remember { mutableStateOf(userPreferences.userId ?: "") }
    // State for search query
    var searchQuery by remember { mutableStateOf("") }

    // State for search results
    var searchResults by remember { mutableStateOf<List<Restaurant>>(emptyList()) }

    // State for loading status
    var isLoading by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    // State for search submission
    var hasSearched by remember { mutableStateOf(false) }
    // State to control WebView visibility
    var webViewVisible by remember { mutableStateOf(false) }


    // Function to make API call
    fun searchRestaurants(query: String, userPreferences: UserPreferences, context: Context) {
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
        val jsonPayload = mapOf("input" to enhancedQuery)

        Log.d("Search", "Making API call jsonpayload: $jsonPayload")

        val call =  RetrofitInstance.api.getRestaurants(jsonPayload)

        Log.d("Search", "Making API call with query: $enhancedQuery")
        Log.d("Search", "Making API call jsonpayload: $jsonPayload")


        call.enqueue(object : Callback<RecommendationResponse> {
            override fun onResponse(call: Call<RecommendationResponse>, response: Response<RecommendationResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    searchResults = response.body()?.recommendations ?: emptyList()
                    // ✅ Add this line to load markers on the map
                    loadMapWithMarkers(webView, searchResults)
                } else {
                    Log.d("Search", "New API call failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RecommendationResponse>, t: Throwable) {
                isLoading = false
                Log.e("Search", "New API call error: ${t.message}")
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
                                    searchQuery,
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
                        coroutineScope.launch {
                            searchRestaurants(searchQuery, userPreferences, context)
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
            } else if (searchResults.isEmpty() && hasSearched) {
                if (searchQuery.isNotEmpty()) {
                    Text(text = "No restaurants found.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn {
                    items(searchResults) { restaurant ->
                        RestaurantCard(restaurant = restaurant, webView = webView)
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}
@Composable
fun RestaurantCard(restaurant: Restaurant, webView: WebView) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val userPreferences = UserPreferences(context)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                userPreferences.addClickedRestaurant(restaurant.name)
                webView.evaluateJavascript(
                    """
                    (function() {
                        if (typeof markersLayerGroup === 'undefined') {
                            console.log('markersLayerGroup is undefined.');
                            return null;
                        }
                        var markers = markersLayerGroup.getLayers();
                        for (var i = 0; i < markers.length; i++) {
                            if (markers[i].getPopup().getContent().includes("${restaurant.name}")) {
                                map.flyTo(markers[i].getLatLng(), 18, { animate: true, duration: 1.4 });
                                setTimeout(() => { markers[i].openPopup(); }, 1600);
                                return 'success';
                            }
                        }
                        return null;
                    })();
                    """.trimIndent()
                ) { result -> Log.d("WebView", "Marker click triggered: $result") }
            },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { uriHandler.openUri(restaurant.url) }
            ) {
                Icon(
                    imageVector = Icons.Default.Public, // 🌍 World Icon
                    contentDescription = "Website",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Visit Restaurant Page",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.Blue,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            // Address & Phone
            FlowRow {
                InfoRow(Icons.Default.LocationOn, "Address", restaurant.address)
                InfoRow(Icons.Default.Phone, "Phone", restaurant.phone)
            }

            // Cuisines & Meals inline
            FlowRow {
                restaurant.cuisines?.let { InlineInfoList("Cuisines", it) }
                restaurant.meals?.let { InlineInfoList("Meals", it) }

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Price: ")
                        }
                        append(restaurant.price)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                restaurant.features?.let { InlineInfoList("Features", it) }
            }
            // Reviews (only show a few)
//            FlowRow {
//                restaurant.reviews?.take(2)?.let { InlineInfoList("Reviews", it, maxChars = 100) }
//            }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    padding: Dp = 0.dp, // Default padding
    horizontalSpacing: Dp = 2.dp, // Default horizontal spacing
    verticalSpacing: Dp = 1.dp, // Default vertical spacing
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth().padding(padding),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        content()
    }
}

@Composable
fun InlineInfoList(label: String, items: List<String>, maxChars: Int = 50) {
    Text(
        text = "$label: ",
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
    )

    items.forEachIndexed { index, item ->
        val truncatedText = if (item.length > maxChars) item.take(maxChars) + "..." else item
        Text(
            text = if (index < items.size - 1) "$truncatedText; " else truncatedText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}


// Helper function for info rows with icons
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "$value",//"$label: $value",
            style = MaterialTheme.typography.bodySmall
        )
    }
}


fun loadMapWithMarkers(webView: WebView, restaurants: List<Restaurant>) {
    Log.d("Search","loading Map With Markers.")
    val simplifiedRestaurants = restaurants.map {
        mapOf(
            "name" to it.name,
            "address" to it.address,
            "latitude" to (it.latitude ?: 0.0),
            "longitude" to (it.longitude ?: 0.0)
        )
    }
    // Convert lists to JSON
    val restaurantsJson = Gson().toJson(simplifiedRestaurants)

    // Inject JavaScript functions based on API type
    if (simplifiedRestaurants.isNotEmpty()) {
        webView.evaluateJavascript("""
            addMarkersWithCoordinates($restaurantsJson);
            fitBoundsToMarkers();
        """.trimIndent(), null)
    }
    Log.d("Search","loading Map With Markers ended.")
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

