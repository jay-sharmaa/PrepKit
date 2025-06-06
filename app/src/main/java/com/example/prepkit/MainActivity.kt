package com.example.prepkit

import PlantClassifierScreen
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.prepkit.MainNavigationScreen.CompassScreen
import com.example.prepkit.MainNavigationScreen.ContactScreen
import com.example.prepkit.MainNavigationScreen.HomeScreen
import com.example.prepkit.MainNavigationScreen.InfoScreen
import com.example.prepkit.MainNavigationScreen.SurvivalScreen
import com.example.prepkit.ui.theme.PrepKitTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    private val _azimuth = mutableStateOf(0f)
    val azimuth: State<Float> get() = _azimuth

    private var _latitude = mutableStateOf(0f)
    private var _longitude = mutableStateOf(0f)

    val latitude: State<Float> get() = _latitude
    val longitude: State<Float> get() = _longitude

    private val intentFilter = IntentFilter()

    val permissions = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val PermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == false

        if (fineLocationGranted || coarseLocationGranted) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            checkPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPermission() {
        val fineLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation != PackageManager.PERMISSION_GRANTED || coarseLocation != PackageManager.PERMISSION_GRANTED) {
            PermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            )
        }
    }

    private fun getCurrLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 2000L
            fastestInterval = 1000L
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _latitude.value = location.latitude.toFloat()
                    _longitude.value = location.longitude.toFloat()
                    Log.d("Location", _latitude.value.toString() + " " + _longitude.value.toString())
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    var isWifiP2pEnabled = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermission()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        val wifiManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = wifiManager.initialize(this, mainLooper, null)
        getCurrLocation()
        enableEdgeToEdge()

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {

            val wifiViewModel = remember { WifiViewModel() }
            registerReceiver(
                WifiStateReceiver(manager, channel, this, wifiViewModel),
                intentFilter
            )
            SideEffect {
                val window = (this as Activity).window
                window.statusBarColor = Color.Black.toArgb()

                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
            }

            MapLibre.getInstance(applicationContext, null, WellKnownTileServer.MapLibre)

            PrepKitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        azimuth = azimuth,
                        latitude = latitude,
                        longitude = longitude,
                        context = this,
                        viewModel = wifiViewModel,
                        manager = wifiManager,
                        channel = channel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
        }

        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)

            val success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)

                val azimuthRadius = orientation[0]
                val azimuthDegrees = Math.toDegrees(azimuthRadius.toDouble()).toFloat()

                _azimuth.value = (azimuthDegrees + 360) % 360
            }
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberAnimatedNavController(),
    azimuth: State<Float>,
    latitude: State<Float>,
    longitude: State<Float>,
    context: Context,
    viewModel: WifiViewModel,
    manager: WifiP2pManager,
    channel: WifiP2pManager.Channel,
) {
    val currentHomeRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Log.d("Navigation ", currentHomeRoute.toString())
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedNavHost(
            navController = navController,
            startDestination = "homeScreen",
            modifier = Modifier.fillMaxSize().padding(androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues()),
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) }
        ) {
            composable("homeScreen") {
                HomeScreen(azimuth, latitude, longitude)
            }
            composable("compassScreen") {
                CompassScreen(azimuth, latitude, longitude)
            }
            composable("contactScreen") {
                ContactScreen(
                    context = context,
                    viewModel = viewModel,
                    manager = manager,
                    channel = channel
                )
            }
            composable("survivalScreen") {
                InfoScreen()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            CompUi(paddingValues = PaddingValues(), modifier = Modifier) { route ->
                navController.navigate(route)
            }
        }
    }
}

@Composable
fun CompUi(paddingValues: PaddingValues, modifier: Modifier, onImageClick: (String) -> Unit) {
    Column(
        modifier = modifier.padding(paddingValues),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircleIcon(onImageClick = onImageClick)
    }
}

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun CircleIcon(onImageClick: (String) -> Unit) {
    val images = listOf(
        R.drawable.homepin,
        R.drawable.compass,
        R.drawable.antenna,
        R.drawable.campfire
    )

    val positions = listOf(
        Offset(0f, -36f),
        Offset(36f, 0f),
        Offset(0f, 36f),
        Offset(-36f, 0f)
    )

    val routes = listOf(
        "homeScreen",
        "compassScreen",
        "contactScreen",
        "survivalScreen"
    )

    var mapping by remember { mutableStateOf(listOf(0, 1, 2, 3)) }

    fun bringToTop(clickedIndex: Int) {
        val currentPosIndex = mapping[clickedIndex]
        val shift = (4 - currentPosIndex) % 4
        mapping = mapping.map { (it + shift) % 4 }
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )

        images.forEachIndexed { index, resId ->
            val offset = positions[mapping[index]]
            val size = if (index == mapping.indexOf(0)) 48.dp else 36.dp

            Image(
                painter = painterResource(id = resId),
                contentDescription = "Image $index",
                colorFilter = ColorFilter.tint(color = Color.White),
                modifier = Modifier
                    .size(size)
                    .offset(offset.x.dp, offset.y.dp)
                    .clickable {
                        bringToTop(index)
                        onImageClick(routes[index])
                    }
            )
        }
    }
}

@Composable
fun MapLibreCompose(
    modifier: Modifier = Modifier,
    initialPosition: LatLng,
    initialZoom: Double = 10.0,
    styleUrl: String = "https://demotiles.maplibre.org/style.json",
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_CREATE -> mapView?.onCreate(null)
                Lifecycle.Event.ON_START -> mapView?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_STOP -> mapView?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                mapView = this
                getMapAsync { mapLibreMap ->
                    mapLibreMap.cameraPosition = CameraPosition.Builder()
                        .target(initialPosition)
                        .zoom(initialZoom)
                        .build()
                    mapLibreMap.setStyle(styleUrl) { style ->
                        onMapReady(mapLibreMap)
                    }
                }
            }
        }
    )
}