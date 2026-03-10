package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.HomeRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.LoginRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.ScanCardRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppDestinations.START_ROUTE) {
        addHomeDestination(navController)
        addLoginDestination(navController)
        addScanCardDestination(navController)
    }
}

private fun NavGraphBuilder.addHomeDestination(
    navController: NavHostController
) {
    composable(route = AppDestinations.HOME_ROUTE) {
        HomeRoute(
            onScanClick = navController::navigateToScanCard,
            onProfileClick = navController::navigateToLogin
        )
    }
}

private fun NavGraphBuilder.addLoginDestination(
    navController: NavHostController
) {
    composable(route = AppDestinations.LOGIN_ROUTE) {
        LoginRoute(
            onBackClick = navController::navigateBackOrHome,
            onHomeClick = navController::navigateHome
        )
    }
}

private fun NavGraphBuilder.addScanCardDestination(
    navController: NavHostController
) {
    composable(route = AppDestinations.SCAN_CARD_ROUTE) {
        ScanCardRoute(
            onBackClick = navController::navigateBackOrHome,
            onScanCardClick = navController::navigateHome
        )
    }
}

private fun NavHostController.navigateToScanCard() {
    navigateTopLevel(AppDestinations.SCAN_CARD_ROUTE)
}

private fun NavHostController.navigateToLogin() {
    navigateTopLevel(AppDestinations.LOGIN_ROUTE)
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateHome() {
    val poppedToHome = popBackStack(AppDestinations.HOME_ROUTE, inclusive = false)
    if (poppedToHome) return

    navigate(AppDestinations.HOME_ROUTE) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBackOrHome() {
    if (!popBackStack()) {
        navigateHome()
    }
}
