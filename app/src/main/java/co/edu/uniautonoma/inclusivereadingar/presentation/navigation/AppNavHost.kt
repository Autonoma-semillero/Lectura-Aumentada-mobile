package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.presentation.student.DomanSessionRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.SessionSummaryScreen
import co.edu.uniautonoma.inclusivereadingar.presentation.student.StudentLoginRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.ThemesRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.SessionViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.SessionViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.CategoryCardsRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.CreateEditThemeRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.CreateWordCardRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.EditWordCardRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.ManageThemesRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.TeacherDomanPlansRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.TeacherLoginRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.TeacherStudentProgressRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.TeacherStudentsRoute
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.WordCardCategoryListRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = context.appContainer()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(container.authRepository)
    )
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(sessionState.session, sessionState.isLoading, currentRoute) {
        if (sessionState.isLoading) {
            return@LaunchedEffect
        }

        if (sessionState.session == null) {
            if (!currentRoute.isLoginRoute()) {
                navController.navigate(AppDestinations.LOGIN_ROUTE) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (currentRoute.isLoginRoute() || currentRoute == null) {
            if (sessionState.session?.user?.roles.orEmpty().any { it == "teacher" || it == "admin" }) {
                navController.navigateToTeacherThemes()
            } else {
                val ongoing = container.sessionStore.getOngoingSession()
                if (ongoing != null) {
                    navController.navigateToDomanSession(ongoing.categoryId, ongoing.categoryName)
                } else {
                    navController.navigateToThemes()
                }
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
                onLoginSuccess = {},
                onTeacherLoginClick = { navController.navigate(AppDestinations.TEACHER_LOGIN_ROUTE) }
            )
        }

        composable(route = AppDestinations.TEACHER_LOGIN_ROUTE) {
            TeacherLoginRoute(
                onLoginSuccess = {},
                onStudentLoginClick = { navController.popBackStack() }
            )
        }

        composable(route = AppDestinations.THEMES_ROUTE) {
            ThemesRoute(
                onThemeClick = { category -> navController.navigateToDomanSession(category.id, category.name) },
                onLogoutClick = { showLogoutDialog = true }
            )
        }

        composable(route = AppDestinations.TEACHER_THEMES_ROUTE) {
            ManageThemesRoute(
                onLogoutClick = { showLogoutDialog = true },
                onCreateThemeClick = { navController.navigateToTeacherThemeForm() },
                onEditThemeClick = { themeId -> navController.navigateToTeacherThemeForm(themeId) },
                onWordCardsClick = navController::navigateToTeacherWordCards,
                onStudentsClick = navController::navigateToTeacherStudents
            )
        }

        composable(route = AppDestinations.TEACHER_STUDENTS_ROUTE) {
            TeacherStudentsRoute(
                onBack = navController::navigateBackOrTeacherThemes,
                onThemesClick = navController::navigateToTeacherThemes,
                onWordCardsClick = navController::navigateToTeacherWordCards,
                onPlansClick = navController::navigateToTeacherPlan,
                onProgressClick = navController::navigateToTeacherProgress
            )
        }

        composable(
            route = AppDestinations.TEACHER_DOMAN_PLAN_ROUTE,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("studentName") { type = NavType.StringType }
            )
        ) { entry ->
            TeacherDomanPlansRoute(
                studentId = entry.arguments?.getString("studentId").orEmpty(),
                studentName = entry.arguments?.getString("studentName").orEmpty(),
                onBack = navController::navigateBackOrTeacherStudents,
                onStudentsClick = navController::navigateToTeacherStudents,
                onThemesClick = navController::navigateToTeacherThemes,
                onWordCardsClick = navController::navigateToTeacherWordCards
            )
        }

        composable(
            route = AppDestinations.TEACHER_PROGRESS_ROUTE,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("studentName") { type = NavType.StringType }
            )
        ) { entry ->
            TeacherStudentProgressRoute(
                studentId = entry.arguments?.getString("studentId").orEmpty(),
                studentName = entry.arguments?.getString("studentName").orEmpty(),
                onBack = navController::navigateBackOrTeacherStudents,
                onStudentsClick = navController::navigateToTeacherStudents,
                onThemesClick = navController::navigateToTeacherThemes,
                onWordCardsClick = navController::navigateToTeacherWordCards
            )
        }

        composable(
            route = AppDestinations.TEACHER_THEME_FORM_ROUTE,
            arguments = listOf(
                navArgument("themeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            CreateEditThemeRoute(
                themeId = entry.arguments?.getString("themeId"),
                onBack = navController::navigateBackOrTeacherThemes,
                onWordCardsClick = navController::navigateToTeacherWordCards
            )
        }

        composable(route = AppDestinations.TEACHER_WORD_CARD_ROUTE) {
            WordCardCategoryListRoute(
                onBack = navController::navigateBackOrTeacherThemes,
                onCategoryClick = { category ->
                    navController.navigate(
                        AppDestinations.teacherCategoryCardsRoute(category.id, category.name)
                    )
                },
                onThemesClick = navController::navigateToTeacherThemes,
                onStudentsClick = navController::navigateToTeacherStudents
            )
        }

        composable(
            route = AppDestinations.TEACHER_CATEGORY_CARDS_ROUTE,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val categoryName = entry.arguments?.getString("categoryName").orEmpty()
            CategoryCardsRoute(
                categoryId = categoryId,
                categoryName = categoryName,
                onBack = navController::navigateBackOrTeacherWordCards,
                onCreateCardClick = { catId ->
                    navController.navigate(AppDestinations.teacherCreateWordCardRoute(catId))
                },
                onEditCardClick = { cardId ->
                    navController.navigate(AppDestinations.teacherEditWordCardRoute(cardId))
                },
                onThemesClick = navController::navigateToTeacherThemes,
                onStudentsClick = navController::navigateToTeacherStudents,
                onWordCardsClick = navController::navigateToTeacherWordCards
            )
        }

        composable(
            route = AppDestinations.TEACHER_CREATE_WORD_CARD_ROUTE,
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId")
            CreateWordCardRoute(
                preSelectedCategoryId = categoryId,
                onBack = navController::navigateBackOrTeacherWordCards,
                onThemesClick = navController::navigateToTeacherThemes,
                onStudentsClick = navController::navigateToTeacherStudents,
                onWordCardsClick = navController::navigateToTeacherWordCards
            )
        }

        composable(
            route = AppDestinations.TEACHER_EDIT_WORD_CARD_ROUTE,
            arguments = listOf(
                navArgument("cardId") { type = NavType.StringType }
            )
        ) { entry ->
            val cardId = entry.arguments?.getString("cardId").orEmpty()
            EditWordCardRoute(
                cardId = cardId,
                onBack = navController::navigateBackOrTeacherWordCards,
                onThemesClick = navController::navigateToTeacherThemes,
                onStudentsClick = navController::navigateToTeacherStudents,
                onWordCardsClick = navController::navigateToTeacherWordCards
            )
        }

        composable(
            route = AppDestinations.DOMAN_SESSION_ROUTE,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { entry ->
            val categoryId = entry.arguments?.getString("categoryId").orEmpty()
            val categoryName = entry.arguments?.getString("categoryName").orEmpty()
            DomanSessionRoute(
                categoryId = categoryId,
                categoryName = categoryName,
                onBackClick = navController::navigateBackOrThemes,
                onSessionCompleted = { cardsCount ->
                    navController.navigate(AppDestinations.sessionSummaryRoute(categoryName, cardsCount)) {
                        popUpTo(AppDestinations.THEMES_ROUTE)
                    }
                }
            )
        }

        composable(
            route = AppDestinations.SESSION_SUMMARY_ROUTE,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("cardsCount") { type = NavType.IntType }
            )
        ) { entry ->
            SessionSummaryScreen(
                categoryName = entry.arguments?.getString("categoryName").orEmpty(),
                cardsCount = entry.arguments?.getInt("cardsCount") ?: 0,
                onFinish = navController::navigateToThemes
            )
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        sessionViewModel.logout()
                    }
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun NavHostController.navigateToThemes() {
    navigate(AppDestinations.THEMES_ROUTE) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToTeacherThemes() {
    navigate(AppDestinations.TEACHER_THEMES_ROUTE) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToTeacherStudents() {
    navigate(AppDestinations.TEACHER_STUDENTS_ROUTE) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToDomanSession(categoryId: String, categoryName: String) {
    navigate(AppDestinations.domanSessionRoute(categoryId, categoryName)) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToTeacherThemeForm(themeId: String? = null) {
    navigate(AppDestinations.teacherThemeFormRoute(themeId)) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToTeacherWordCards() {
    navigate(AppDestinations.TEACHER_WORD_CARD_ROUTE) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBackOrTeacherWordCards() {
    if (!popBackStack()) {
        navigateToTeacherWordCards()
    }
}

private fun NavHostController.navigateToTeacherPlan(student: AppUser) {
    navigate(AppDestinations.teacherDomanPlanRoute(student.id, student.displayName ?: student.email)) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToTeacherProgress(student: AppUser) {
    navigate(AppDestinations.teacherProgressRoute(student.id, student.displayName ?: student.email)) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateBackOrThemes() {
    if (!popBackStack()) {
        navigateToThemes()
    }
}

private fun NavHostController.navigateBackOrTeacherThemes() {
    if (!popBackStack()) {
        navigateToTeacherThemes()
    }
}

private fun NavHostController.navigateBackOrTeacherStudents() {
    if (!popBackStack()) {
        navigateToTeacherStudents()
    }
}

private fun String?.isLoginRoute(): Boolean {
    return this == AppDestinations.LOGIN_ROUTE || this == AppDestinations.TEACHER_LOGIN_ROUTE
}


