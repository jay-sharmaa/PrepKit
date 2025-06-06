package com.example.prepkit.MainNavigationScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prepkit.R

@Composable
fun CompassScreen(azimuth: State<Float>, latitude: State<Float>, longitude: State<Float>) {
    val animatedAzimuth by animateFloatAsState(
        targetValue = -azimuth.value,
        label = "compass-rotation"
    )

    val degLatitude : Int = latitude.value.toInt()
    val arcMinuteLatitude : Float = (latitude.value - degLatitude) * 100

    val degLongitude : Int = longitude.value.toInt()
    val arcMinuteLongitude : Float = (longitude.value - degLongitude) * 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.compass),
            contentDescription = "Compass Arrow",
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .rotate(animatedAzimuth),
            tint = Color.White
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .border(width = 1.dp, color = Color.Black)
                .padding(6.dp)
        ) {

            Text(
                text = "Lat: ${degLatitude}° ${"%.0f".format(arcMinuteLatitude)}'",
                color = Color.White,
                fontSize = 20.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .border(width = 1.dp, color = Color.Black)
                .padding(6.dp)
        ) {
            Text(
                text = "Long: ${degLongitude}° ${"%.0f".format(arcMinuteLongitude)}'",
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }
}