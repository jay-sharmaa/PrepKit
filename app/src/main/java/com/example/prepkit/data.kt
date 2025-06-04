package com.example.prepkit

data class Guide(
    val place: String,
    val animals: List<String>,
    val climate: List<String>,
    val safety: List<String>
)

val places : List<Guide> = listOf(
    Guide(
        place = "Forest",
        animals = listOf<String>("Leopard", "Indian Eagle", "Indian Cobra", "Sambar Deer", "Barking Deer"),
        climate = listOf<String>("2000-6000 mm", "25-30", "Very high"),
        safety = listOf<String>("Carry Rain Cover or Poncho", "Emergency Whistle", "Insect Repellent")
    ),
    Guide(
        place = "Hill",
        animals = listOf<String>("Snow Leopard", "Himalayan Black Bear", "Musk Deer", "Himalayan Monal", "Red Panda"),
        climate = listOf<String>("500-1500 mm", "-5 to 25°C", "Cool and breezy"),
        safety = listOf<String>("Warm layered clothing", "Sturdy trekking boots", "Altitude sickness kit", "Flashlight or Headlamp", "First Aid Kit")
    ),
    Guide(
        place = "Trek",
        animals = listOf<String>("Mountain Goat", "Himalayan Griffon", "Wild Boar", "Langur", "Monitor Lizard"),
        climate = listOf<String>("Variable – 800 to 3000 mm", "5 to 35°C", "Depends on altitude and season"),
        safety = listOf<String>(
            "Proper trekking shoes with grip",
            "Adequate hydration (2-3L water)",
            "Energy bars or dry snacks",
            "Map or GPS device",
            "Sun protection (hat, sunglasses, sunscreen)",
            "Basic first aid kit"
        )
    )
)