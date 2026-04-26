package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

object AppDestinations {
    const val LOGIN_ROUTE = "student_login"
    const val TEACHER_LOGIN_ROUTE = "teacher_login"
    const val THEMES_ROUTE = "themes"
    const val TEACHER_THEMES_ROUTE = "teacher_themes"
    const val TEACHER_THEME_FORM_ROUTE = "teacher_theme_form?themeId={themeId}"
    const val TEACHER_WORD_CARD_ROUTE = "teacher_word_card"
    const val START_ROUTE = LOGIN_ROUTE
    const val SCAN_CARD_ROUTE = "scan_card/{categoryId}/{categoryName}"
    const val PRACTICE_ROUTE = "practice/{categoryId}/{categoryName}"

    fun scanCardRoute(categoryId: String, categoryName: String): String {
        return "scan_card/$categoryId/${android.net.Uri.encode(categoryName)}"
    }

    fun practiceRoute(categoryId: String, categoryName: String): String {
        return "practice/$categoryId/${android.net.Uri.encode(categoryName)}"
    }

    fun teacherThemeFormRoute(themeId: String? = null): String {
        return if (themeId != null) {
            "teacher_theme_form?themeId=$themeId"
        } else {
            "teacher_theme_form"
        }
    }
}
