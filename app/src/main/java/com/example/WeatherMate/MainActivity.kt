package com.example.WeatherMate

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.Text


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.weatherapphw2.R
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.delay


import java.net.URLEncoder
import java.net.UnknownHostException

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen {
                    showSplash = false
                }
            } else {
                WeatherApp()
            }
        }

    }
}

data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
)

data class Coord(val lon: Double, val lat: Double)
data class Weather(val id: Int, val main: String, val description: String, val icon: String)
data class Main(val temp: Double, val feels_like: Double, val temp_min: Double, val temp_max: Double, val pressure: Int, val humidity: Int, val sea_level: Int, val grnd_level: Int)
data class Wind(val speed: Double, val deg: Int)
data class Clouds(val all: Int)
data class Sys(val type: Int, val id: Int, val country: String, val sunrise: Long, val sunset: Long)


//@Composable
//fun WeatherApp() {
//    var cityName by remember { mutableStateOf(TextFieldValue("")) } // For storing the city name
//    var weatherData by remember { mutableStateOf<WeatherResponse?>(null) }
//    val scope = rememberCoroutineScope()
//
//    Column(Modifier.fillMaxSize().padding(16.dp)) {
//        // City name input
//        Text("Enter City Name", style = MaterialTheme.typography.titleMedium)
//        BasicTextField(
//            value = cityName,
//            onValueChange = { cityName = it },
//            modifier = Modifier
//                .padding(8.dp)
//                .fillMaxWidth()
//                .height(56.dp),
//            decorationBox = { innerTextField ->
//                Box(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
//                        .fillMaxWidth(),
//                    contentAlignment = Alignment.CenterStart
//                ) {
//                    if (cityName.text.isEmpty()) {
//                        Text("City Name", style = MaterialTheme.typography.bodyLarge)
//                    }
//                    innerTextField()
//                }
//            }
//        )
//
//        Spacer(Modifier.height(16.dp))
//
//        // Button to fetch weather data
//        Button(onClick = {
//            Log.d("WeatherApp", "Button clicked, fetching weather data for: ${cityName.text}")
//            scope.launch {
//                if (cityName.text.isNotEmpty()) {
//                    weatherData = getWeather(cityName.text)
//                }
//            }
//        }) {
//            Text("Get Weather")
//        }
//
//        Spacer(Modifier.height(16.dp))
//
//        // Display weather data
//        weatherData?.let { data ->
//            Text("City: ${data.name}")
//            Text("Temperature: ${data.main.temp}°C")
//            Text("Humidity: ${data.main.humidity}%")
//            Text("Weather: ${data.weather[0].main} - ${data.weather[0].description}")
//            Text("Wind Speed: ${data.wind.speed} m/s")
//        }
//    }
//}





// Add these data classes at the top of the file
data class Province(val name: String, val cities: List<String>)

val iranProvinces = listOf(
    Province("Tehran", listOf("Tehran", "Varamin", "Damavand", "Firuzkuh")),
    Province("Isfahan", listOf("Isfahan", "Kashan", "Najafabad", "Shahreza")),
    Province("Fars", listOf("Shiraz", "Marvdasht", "Kazerun", "Jahrom")),
    Province("Khuzestan", listOf("Ahvaz", "Abadan", "Khorramshahr", "Dezful")),
    Province("East Azerbaijan", listOf("Tabriz", "Maragheh", "Marand", "Mianeh"))
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherApp() {
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var weatherData by remember { mutableStateOf<WeatherResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }


    var deviceLocation by remember { mutableStateOf<Location?>(null) }
    // 2. Permission state for ACCESS_FINE_LOCATION
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    // 3. FusedLocationProviderClient to get location
    val fusedLocationClient = LocationServices
        .getFusedLocationProviderClient(LocalContext.current)

    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("Click the button to get weather") }
    var fetchedCityName by remember { mutableStateOf<String?>(null) }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        var showTextField by remember { mutableStateOf(false) }
//        var text by remember { mutableStateOf("") }
//
//        Button(onClick = { showTextField = true }) {
//            Text("Current Location Weather", style = MaterialTheme.typography.titleSmall)
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//
//        if (showTextField) {
//            TextField(
//                value = text,
//                onValueChange = { text = it },
//                label = { Text("Enter something") },
//                modifier = Modifier.fillMaxWidth()
//            )
//        }

        Button(onClick = {
            if (permissionState.status.isGranted) {
                statusMessage = "Permission granted. Getting location..."

                // **Platform check** to satisfy Lint
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                statusMessage = "Got location: (${location.latitude}, ${location.longitude})"

                                // Launch coroutine to fetch weather
                                scope.launch {
                                    errorMessage = null
                                    isLoading = true
                                    weatherData = getWeatherByCoordinates(location.latitude, location.longitude)
                                    isLoading = false
                                    val cityNameFromAPI = weatherData?.name
                                    val matchedProvince = iranProvinces.find { province ->
                                        province.cities.any { it.equals(cityNameFromAPI, ignoreCase = true) }
                                    }

                                    selectedProvince = matchedProvince
                                    selectedCity = cityNameFromAPI
                                }
                            } else {
                                statusMessage = "Location is null"
                            }
                        }
                        .addOnFailureListener {
                            statusMessage = "Location fetch failed: ${it.localizedMessage}"
                        }
                }
            } else {
                permissionState.launchPermissionRequest()
            }
        }) {
            Text("Get Device Location")
        }

        Spacer(Modifier.height(16.dp))
        Text(statusMessage)

        deviceLocation?.let { loc ->
            Text("Lat: ${loc.latitude}, Lon: ${loc.longitude}")
        }
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }







        // Province Selector
        ExposedDropdownMenuBox(
            expanded = provinceExpanded,
            onExpandedChange = { provinceExpanded = it }
        ) {
            TextField(
                value = selectedProvince?.name ?: "Select Province",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = provinceExpanded
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )

            ExposedDropdownMenu(
                expanded = provinceExpanded,
                onDismissRequest = { provinceExpanded = false }
            ) {
                iranProvinces.forEach { province ->
                    DropdownMenuItem(
                        text = { Text(province.name) },
                        onClick = {
                            selectedProvince = province
                            selectedCity = null
                            provinceExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // City Selector
        selectedProvince?.let { province ->
            ExposedDropdownMenuBox(
                expanded = cityExpanded,
                onExpandedChange = { cityExpanded = it }
            ) {
                TextField(
                    value = selectedCity ?: "Select City",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = cityExpanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = cityExpanded,
                    onDismissRequest = { cityExpanded = false }
                ) {
                    province.cities.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city) },
                            onClick = {
                                selectedCity = city
                                cityExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Weather Button
        Button(
            onClick = {
                selectedCity?.let { city ->
                    scope.launch {
                        weatherData = null
                        errorMessage = null
                        try {
                            isLoading = true
                            weatherData = getWeatherWithFallback(city)
                            isLoading = false
                            weatherData?.let {
                                if (it.name.isNullOrEmpty()) {
                                    errorMessage = "Data not found for $city"
                                    weatherData = null
                                }
                            } ?: run {
                                errorMessage = "No weather data available"
                            }
                        } catch (e: Exception) {
                            errorMessage = when (e) {
                                is SocketTimeoutException -> "Request timed out"
                                is UnknownHostException -> "No internet connection"
                                else -> "Error: ${e.localizedMessage}"
                            }
                        }
                    }
                }
            },
            enabled = selectedCity != null,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Show Weather", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }

        // Error message
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Weather Display
        weatherData?.let { data ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "$selectedCity",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "${data.main?.temp?.let { String.format("%.1f", it) } ?: "--"}°C",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercaseChar() } ?: "N/A",
                        style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic),
                        modifier = Modifier.padding(top = 4.dp)
                    )

//                    WeatherInfoRow("City", data.name ?: "N/A")
                    WeatherInfoRow("Feels Like", "${data.main?.feels_like ?: "N/A"}°C")
                    WeatherInfoRow("Min Temp", "${data.main?.temp_min ?: "N/A"}°C")
                    WeatherInfoRow("Max Temp", "${data.main?.temp_max ?: "N/A"}°C")
                    WeatherInfoRow("Humidity", "${data.main?.humidity ?: "N/A"}%")
                    WeatherInfoRow("Pressure", "${data.main?.pressure ?: "N/A"} hPa")
                    WeatherInfoRow("Visibility", "${(data.visibility ?: 0) / 1000} km")
                    WeatherInfoRow("Wind Speed", "${data.wind?.speed ?: "N/A"} m/s")
                    WeatherInfoRow("Wind Direction", "${data.wind?.deg ?: "N/A"}°")
                }

            }
        }
    }
        }
}


@Composable
fun WeatherInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

suspend fun getWeatherWithFallback(city: String): WeatherResponse? {
    return try {
        // First try with country code
        getWeather("$city,IR") ?: getWeather(city)
    } catch (e: Exception) {
        null
    }
}




// Function to fetch weather data from the OpenWeather API
suspend fun getWeather(city: String): WeatherResponse? {
    return try {
        val apiKey = "35622ba50b71bcc91267f3bed1f5eaf7"
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val urlString = "https://api.openweathermap.org/data/2.5/weather?q=$encodedCity&appid=$apiKey&units=metric"

        Log.d("WeatherApp", "Request URL: $urlString")

        val response = withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    val errorStream = InputStreamReader(connection.errorStream).readText()
                    Log.e("WeatherApp", "HTTP Error: ${connection.responseCode} - $errorStream")
                    return@withContext null
                }

                InputStreamReader(connection.inputStream).readText()
            } finally {
                connection.disconnect()
            }
        } ?: return null

        Log.d("WeatherApp", "Raw response: $response")

        // Parse response
        Gson().fromJson(response, WeatherResponse::class.java)?.also {
            Log.d("WeatherApp", "Parsed data: $it")
        }
    } catch (e: Exception) {
        Log.e("WeatherApp", "Exception: ${e.javaClass.simpleName} - ${e.message}")
        throw e
    }
}


suspend fun getWeatherByCoordinates(lat: Double, lon: Double): WeatherResponse? {

    return try {
        val apiKey = "35622ba50b71bcc91267f3bed1f5eaf7"
        val urlString = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

        Log.d("WeatherApp", "Request URL: $urlString")

        val response = withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    val errorStream = InputStreamReader(connection.errorStream).readText()
                    Log.e("WeatherApp", "HTTP Error: ${connection.responseCode} - $errorStream")
                    return@withContext null
                }

                InputStreamReader(connection.inputStream).readText()
            } finally {
                connection.disconnect()
            }
        } ?: return null

        Log.d("WeatherApp", "Raw response: $response")

        // Parse response
        Gson().fromJson(response, WeatherResponse::class.java)?.also {
            Log.d("WeatherApp", "Parsed data: $it")
        }
    } catch (e: Exception) {
        Log.e("WeatherApp", "Exception: ${e.javaClass.simpleName} - ${e.message}")
        throw e
    }

}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var alpha by remember { mutableStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = tween(durationMillis = 800)
    )

    LaunchedEffect(Unit) {
        alpha = 1f // Fade in
        delay(2000)
        alpha = 0f // Fade out
        delay(800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB3E5FC)) // sky blue
            .graphicsLayer { this.alpha = animatedAlpha },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.cloudy),
                contentDescription = "Weather Icon",
                tint = Color.White,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "WeatherMate☀️",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
            )
            Text(
                "By Yaser Zarifi",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                modifier = Modifier
                    .padding(16.dp)
            )
        }
    }
}
