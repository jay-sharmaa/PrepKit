package com.example.prepkit.MainNavigationScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prepkit.Guide
import com.example.prepkit.places

@Composable
fun SurviveKnowledge(dataId : String){
    val place: List<Guide> = places;

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when(dataId){
                "0" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PlaceHeader("Danger \uD83D\uDD34    " + place[0].place + "  \uD83D\uDFE2 Safe")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimalDanger(modifier = Modifier.weight(1f), names = place[0].animals)
                    Spacer(modifier = Modifier.height(12.dp))
                    ClimateDanger(modifier = Modifier.weight(1f), names = place[0].climate)
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyMeasures(modifier = Modifier.weight(1f), names = place[0].safety)
                }
                "1" -> {
                    PlaceHeader("Danger \uD83D\uDD34    " + place[1].place + "  \uD83D\uDFE2 Safe")
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimalDanger(modifier = Modifier.weight(1f), names = place[1].animals)
                    Spacer(modifier = Modifier.height(12.dp))
                    ClimateDanger(modifier = Modifier.weight(1f), names = place[1].climate)
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyMeasures(modifier = Modifier.weight(1f), names = place[1].safety)
                }
                "2" -> {
                    PlaceHeader("Danger \uD83D\uDD34    " + place[2].place + "  \uD83D\uDFE2 Safe")
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimalDanger(modifier = Modifier.weight(1f), names = place[2].animals)
                    Spacer(modifier = Modifier.height(12.dp))
                    ClimateDanger(modifier = Modifier.weight(1f), names = place[2].climate)
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyMeasures(modifier = Modifier.weight(1f), names = place[2].safety)
                }
            }
        }
    }
}


@Composable
fun PlaceHeader(placeName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2C2C2C),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            val parts = placeName.split("🔴", "🟢")
            if (parts.size >= 3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Danger 🔴",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = parts[1].trim(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "🟢 Safe",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Text(
                    text = placeName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AnimalDanger(modifier: Modifier = Modifier, names: List<String>) {
    Card(
        modifier = modifier
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = "🦁 Animals in This Region",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn {
                itemsIndexed(names) { index, animal ->
                    val backgroundColor = if (index < 3) {
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF5252))
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = animal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClimateDanger(modifier: Modifier = Modifier, names: List<String>) {
    Card(
        modifier = modifier
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = "🌡️ Climate Conditions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn {
                items(names) { climateValue ->
                    val isDangerous = isClimateDangerous(climateValue)
                    val backgroundColor = if (isDangerous) {
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF5252))
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = climateValue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

fun isClimateDangerous(value: String): Boolean {
    val lower = value.lowercase()

    return when {
        "very high" in lower -> true
        "high" in lower && "very" !in lower -> false
        value.contains("3000") || value.contains("4000") || value.contains("6000") -> true
        value.contains("35") || value.contains("40") || value.contains("0") || value.contains("-") -> true
        else -> false
    }
}

@Composable
fun SafetyMeasures(modifier: Modifier = Modifier, names: List<String>) {
    Card(
        modifier = modifier
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = "🛡️ Safety Measures",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn {
                items(names) { safetyTip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "• $safetyTip",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}