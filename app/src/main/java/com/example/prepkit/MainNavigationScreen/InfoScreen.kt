package com.example.prepkit.MainNavigationScreen

import PlantClassifierScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun InfoScreen(){
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "SurvivalScreen"
    ) {
        composable("SurvivalScreen") { SurvivalScreen(navController) }
        composable(
            route = "SurviveKnowledge/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dataId = backStackEntry.arguments?.getString("id")
            SurviveKnowledge(dataId!!)
        }
        composable("PlantsClassification") {
            PlantClassifierScreen()
        }
    }
}