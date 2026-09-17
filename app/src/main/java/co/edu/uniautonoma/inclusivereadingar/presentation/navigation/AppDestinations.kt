package co.edu.uniautonoma.inclusivereadingar.presentation.navigation

object AppDestinations {
    const val LOGIN_ROUTE = "student_login"
    const val TEACHER_LOGIN_ROUTE = "teacher_login"
    const val THEMES_ROUTE = "themes"
    const val DOMAN_SESSION_ROUTE = "doman_session/{categoryId}/{categoryName}"
    const val SESSION_SUMMARY_ROUTE = "session_summary/{categoryName}/{cardsCount}"
    const val TEACHER_THEMES_ROUTE = "teacher_themes"
    const val TEACHER_STUDENTS_ROUTE = "teacher_students"
    const val TEACHER_GROUPS_ROUTE = "teacher_groups"
    const val TEACHER_PLAN_ASSIGNMENT_ROUTE =
        "teacher_plan_assignment?studentId={studentId}&studentName={studentName}"
    const val TEACHER_DOMAN_PLAN_ROUTE = "teacher_doman_plan/{studentId}/{studentName}"
    const val TEACHER_PROGRESS_ROUTE = "teacher_progress/{studentId}/{studentName}"
    const val TEACHER_THEME_FORM_ROUTE = "teacher_theme_form?themeId={themeId}"
    const val TEACHER_WORD_CARD_ROUTE = "teacher_word_card"
    const val TEACHER_CATEGORY_CARDS_ROUTE = "teacher_category_cards/{categoryId}/{categoryName}"
    const val TEACHER_CREATE_WORD_CARD_ROUTE = "teacher_create_word_card?categoryId={categoryId}"
    const val TEACHER_EDIT_WORD_CARD_ROUTE = "teacher_edit_word_card/{cardId}"
    const val TEACHER_COMPLETED_CARDS_ROUTE = "teacher_completed_cards/{studentId}/{categoryId}/{categoryName}/{phase2Ready}"
    const val START_ROUTE = LOGIN_ROUTE
    const val PRACTICE_ROUTE = "webar_practice"

    fun domanSessionRoute(categoryId: String, categoryName: String): String {
        return "doman_session/$categoryId/${android.net.Uri.encode(categoryName)}"
    }

    fun sessionSummaryRoute(categoryName: String, cardsCount: Int): String {
        return "session_summary/${android.net.Uri.encode(categoryName)}/$cardsCount"
    }

    fun teacherCategoryCardsRoute(categoryId: String, categoryName: String): String {
        return "teacher_category_cards/$categoryId/${android.net.Uri.encode(categoryName)}"
    }

    fun teacherCreateWordCardRoute(categoryId: String? = null): String {
        return if (categoryId != null) "teacher_create_word_card?categoryId=$categoryId"
        else "teacher_create_word_card"
    }

    fun teacherEditWordCardRoute(cardId: String): String = "teacher_edit_word_card/$cardId"

    fun teacherCompletedCardsRoute(
        studentId: String,
        categoryId: String,
        categoryName: String,
        phase2Ready: Boolean
    ): String = "teacher_completed_cards/$studentId/$categoryId/${android.net.Uri.encode(categoryName)}/$phase2Ready"

    fun teacherThemeFormRoute(themeId: String? = null): String {
        return if (themeId != null) {
            "teacher_theme_form?themeId=$themeId"
        } else {
            "teacher_theme_form"
        }
    }

    fun teacherDomanPlanRoute(studentId: String, studentName: String): String {
        return "teacher_doman_plan/$studentId/${android.net.Uri.encode(studentName)}"
    }

    fun teacherPlanAssignmentRoute(studentId: String? = null, studentName: String? = null): String {
        if (studentId.isNullOrBlank()) return "teacher_plan_assignment"
        return "teacher_plan_assignment?studentId=${android.net.Uri.encode(studentId)}" +
            "&studentName=${android.net.Uri.encode(studentName.orEmpty())}"
    }

    fun teacherProgressRoute(studentId: String, studentName: String): String {
        return "teacher_progress/$studentId/${android.net.Uri.encode(studentName)}"
    }
}
