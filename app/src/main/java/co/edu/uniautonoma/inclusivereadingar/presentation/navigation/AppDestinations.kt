package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

object AppDestinations {
    const val LOGIN_ROUTE = "student_login"
    const val THEMES_ROUTE = "themes"
    const val START_ROUTE = LOGIN_ROUTE
    const val SCAN_CARD_ROUTE = "scan_card/{categoryId}/{categoryName}"
    const val PRACTICE_ROUTE = "practice/{categoryId}/{categoryName}"

    fun scanCardRoute(categoryId: String, categoryName: String): String {
        return "scan_card/$categoryId/${android.net.Uri.encode(categoryName)}"
    }

    fun practiceRoute(categoryId: String, categoryName: String): String {
        return "practice/$categoryId/${android.net.Uri.encode(categoryName)}"
    }
}
