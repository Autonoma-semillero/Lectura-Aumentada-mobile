package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.WebArRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppDestinations.WEB_AR_ROUTE) {
        composable(route = AppDestinations.WEB_AR_ROUTE) { WebArRoute() }
    }
}
