package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.domain.model.CategorySummary
import co.edu.uniautonoma.inclusivereadingar.presentation.screens.ScanCardRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.PracticeCardsRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.StudentLoginRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.ThemesRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.SessionViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.SessionViewModelFactory

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(context.appContainer().authRepository)
    )
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(sessionState.session, sessionState.isLoading) {
        if (sessionState.isLoading) {
            return@LaunchedEffect
        }

        if (sessionState.session == null) {
            if (currentRoute != AppDestinations.LOGIN_ROUTE) {
                navController.navigate(AppDestinations.LOGIN_ROUTE) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (currentRoute == AppDestinations.LOGIN_ROUTE || currentRoute == null) {
            navController.navigate(AppDestinations.THEMES_ROUTE) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (sessionState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = AppDestinations.START_ROUTE) {
        composable(route = AppDestinations.LOGIN_ROUTE) {
            StudentLoginRoute(
                onLoginSuccess = { navController.navigateToThemes() }
            )
        }

        composable(route = AppDestinations.THEMES_ROUTE) {
            ThemesRoute(
                onThemeClick = { category -> navController.navigateToPractice(category) },
                onLogoutClick = sessionViewModel::logout
            )
        }

        composable(
            route = AppDestinations.SCAN_CARD_ROUTE,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val categoryName = entry.arguments?.getString("categoryName").orEmpty()
            ScanCardRoute(
                categoryName = categoryName,
                onBackClick = navController::navigateBackOrThemes,
                onScanCardClick = {
                    navController.navigate(AppDestinations.practiceRoute(categoryId, categoryName))
                }
            )
        }

        composable(
            route = AppDestinations.PRACTICE_ROUTE,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val categoryName = entry.arguments?.getString("categoryName").orEmpty()
            PracticeCardsRoute(
                categoryId = categoryId,
                categoryName = categoryName,
                onBackClick = navController::navigateBackOrThemes
            )
        }
    }
}

private fun NavHostController.navigateToThemes() {
    navigate(AppDestinations.THEMES_ROUTE) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToPractice(category: CategorySummary) {
    navigate(AppDestinations.practiceRoute(category.id, category.name)) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToScanCard(category: CategorySummary) {
    navigate(AppDestinations.scanCardRoute(category.id, category.name)) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBackOrThemes() {
    if (!popBackStack()) {
        navigateToThemes()
    }
}
