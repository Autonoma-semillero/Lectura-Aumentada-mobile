package co.edu.uniautonoma.inclusivereadingar.data.repository

import android.os.Build
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.DomanPlansApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.DomanSessionsApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.StudyPlansApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlan
import co.edu.uniautonoma.inclusivereadingar.domain.model.ActiveStudyPlanCategory
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.BulkPlanGenerationResult
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionHistoryItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.OngoingDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgressSummary
import java.util.Locale

/**
 * Data required by [co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.StudentTodayViewModel]
 * for the logged-in student. Kept as a narrow interface (instead of depending on the concrete
 * [DomanRepository]) so it can be faked in JVM unit tests without a real [SessionStore]/[Context].
 */
interface StudentTodayDataSource {
    suspend fun getActivePlanForCurrentStudent(): ActiveStudyPlan?
    suspend fun getTodayActivitiesForCurrentStudent(activePlan: ActiveStudyPlan): TodayActivitiesResult
}

data class TodayActivitiesResult(
    val activities: List<DailyPlanSummary>,
    val countsLoadFailed: Boolean
)

interface DomanSessionDataSource {
    suspend fun prepareSession(categoryId: String): DomanSession
    suspend fun loadSession(sessionId: String): DomanSession
    suspend fun registerCardShown(sessionId: String, wordCardId: String, displayMs: Int)
    suspend fun registerCardCompleted(sessionId: String, wordCardId: String)
    suspend fun registerCardSkipped(sessionId: String, wordCardId: String)
    suspend fun registerAudioPlayed(sessionId: String, wordCardId: String)
    suspend fun completeSession(sessionId: String): DomanSession
    suspend fun saveOngoingSession(snapshot: OngoingDomanSession)
    suspend fun getOngoingSession(): OngoingDomanSession?
    suspend fun clearOngoingSession()
}

class DomanRepository(
    private val sessionStore: SessionStore,
    private val plansApi: DomanPlansApi,
    private val sessionsApi: DomanSessionsApi,
    private val studyPlansApi: StudyPlansApi
) : DomanSessionDataSource, StudentTodayDataSource {
    override suspend fun prepareSession(categoryId: String): DomanSession {
        val session = requireSession()
        plansApi.generate(
            studentId = session.user.id,
            categoryId = categoryId,
            force = false,
            accessToken = session.accessToken
        )
        val nextSession = sessionsApi.getNext(
            studentId = session.user.id,
            categoryId = categoryId,
            accessToken = session.accessToken
        )
        return sessionsApi.start(nextSession.sessionId, session.accessToken)
    }

    suspend fun getTodayPlan(): DailyPlanSummary {
        val session = requireSession()
        return plansApi.getToday(session.user.id, session.accessToken)
    }

    suspend fun getTodayPlan(studentId: String): DailyPlanSummary {
        val session = requireSession()
        return plansApi.getToday(studentId, session.accessToken)
    }

    suspend fun deletePlan(planId: String) {
        val session = requireSession()
        plansApi.deletePlan(planId, session.accessToken)
    }

    suspend fun getActiveStudyPlan(studentId: String? = null): ActiveStudyPlan? {
        val session = requireSession()
        return studyPlansApi.getActive(
            studentId = studentId ?: session.user.id,
            date = null,
            accessToken = session.accessToken
        )
    }

    suspend fun getTodayPlans(studentId: String): List<DailyPlanSummary> {
        val session = requireSession()
        val today = todayDateString()
        val plans = plansApi.getByDateRange(studentId, today, today, session.accessToken)
        return plans.map { plan ->
            val counts = runCatching {
                sessionsApi.getByPlanId(plan.planId, session.accessToken)
            }.getOrNull()
            if (counts != null) {
                plan.copy(
                    sessionsCount = counts.total,
                    completedSessionsCount = counts.completed,
                    pendingSessionsCount = counts.pending
                )
            } else plan
        }
    }

    override suspend fun getActivePlanForCurrentStudent(): ActiveStudyPlan? = getActiveStudyPlan(studentId = null)

    override suspend fun getTodayActivitiesForCurrentStudent(
        activePlan: ActiveStudyPlan
    ): TodayActivitiesResult {
        val session = requireSession()
        val today = todayDateString()
        var plans = plansApi.getByDateRange(session.user.id, today, today, session.accessToken)
        val missingCategories = missingActiveCategories(plans, activePlan)
        if (missingCategories.isNotEmpty()) {
            missingCategories.forEach { category ->
                plansApi.generate(
                    studentId = session.user.id,
                    categoryId = category.id,
                    force = false,
                    accessToken = session.accessToken
                )
            }
            plans = plansApi.getByDateRange(session.user.id, today, today, session.accessToken)
        }
        val matchingPlans = filterAndEnrichTodayActivities(plans, activePlan)
        var countsLoadFailed = false
        val activities = matchingPlans.map { plan ->
            val counts = runCatching {
                sessionsApi.getByPlanId(plan.planId, session.accessToken)
            }.getOrNull()
            if (counts != null) {
                plan.copy(
                    sessionsCount = counts.total,
                    completedSessionsCount = counts.completed,
                    pendingSessionsCount = counts.pending
                )
            } else {
                countsLoadFailed = true
                plan
            }
        }
        return TodayActivitiesResult(activities = activities, countsLoadFailed = countsLoadFailed)
    }

    private fun todayDateString(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        java.time.LocalDate.now().toString()
    } else {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Calendar.getInstance().time)
    }

    suspend fun generatePlan(studentId: String, categoryId: String? = null, force: Boolean = true): DailyPlanSummary {
        val session = requireSession()
        return plansApi.generate(studentId, categoryId, force, session.accessToken)
    }

    suspend fun generateBulkPlan(
        groupIds: Collection<String>,
        studentIds: Collection<String>,
        categoryId: String?,
        planDate: String? = null,
        targetCardsCount: Int? = null,
        targetSessionsCount: Int? = null,
        displayMs: Int? = null,
        force: Boolean = false
    ): BulkPlanGenerationResult {
        val session = requireSession()
        return plansApi.generateBulk(
            groupIds = groupIds,
            studentIds = studentIds,
            categoryId = categoryId,
            planDate = planDate,
            targetCardsCount = targetCardsCount,
            targetSessionsCount = targetSessionsCount,
            displayMs = displayMs,
            force = force,
            accessToken = session.accessToken
        )
    }

    override suspend fun loadSession(sessionId: String): DomanSession {
        val session = requireSession()
        return sessionsApi.getSession(sessionId, session.accessToken)
    }

    override suspend fun registerCardShown(sessionId: String, wordCardId: String, displayMs: Int) {
        val session = requireSession()
        sessionsApi.registerExposure(
            sessionId = sessionId,
            wordCardId = wordCardId,
            eventType = "card_shown",
            displayMs = displayMs,
            accessToken = session.accessToken
        )
    }

    override suspend fun registerCardCompleted(sessionId: String, wordCardId: String) {
        val session = requireSession()
        sessionsApi.registerExposure(
            sessionId = sessionId,
            wordCardId = wordCardId,
            eventType = "card_completed",
            displayMs = null,
            accessToken = session.accessToken
        )
    }

    override suspend fun registerCardSkipped(sessionId: String, wordCardId: String) {
        val session = requireSession()
        sessionsApi.registerExposure(
            sessionId = sessionId,
            wordCardId = wordCardId,
            eventType = "card_skipped",
            displayMs = null,
            accessToken = session.accessToken
        )
    }

    override suspend fun registerAudioPlayed(sessionId: String, wordCardId: String) {
        val session = requireSession()
        sessionsApi.registerExposure(
            sessionId = sessionId,
            wordCardId = wordCardId,
            eventType = "audio_played",
            displayMs = null,
            accessToken = session.accessToken
        )
    }

    override suspend fun completeSession(sessionId: String): DomanSession {
        val session = requireSession()
        return sessionsApi.complete(sessionId, session.accessToken)
    }

    suspend fun getStudentsHistory(studentId: String): List<DomanSessionHistoryItem> {
        val session = requireSession()
        return sessionsApi.getHistory(studentId, session.accessToken)
    }

    suspend fun getStudentProgressSummary(studentId: String): StudentProgressSummary {
        val session = requireSession()
        return sessionsApi.getProgressSummary(studentId, session.accessToken)
    }

    override suspend fun saveOngoingSession(snapshot: OngoingDomanSession) {
        sessionStore.saveOngoingSession(snapshot)
    }

    override suspend fun getOngoingSession(): OngoingDomanSession? = sessionStore.getOngoingSession()

    override suspend fun clearOngoingSession() {
        sessionStore.clearOngoingSession()
    }

    private suspend fun requireSession(): AuthSession = requireNotNull(sessionStore.getSession()) {
        "A valid session is required"
    }

    fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}

private fun String?.equalsIdentifier(other: String): Boolean =
    this?.equals(other, ignoreCase = true) == true

internal fun filterAndEnrichTodayActivities(
    plans: List<DailyPlanSummary>,
    activePlan: ActiveStudyPlan
): List<DailyPlanSummary> {
    val activeCategoriesById = activePlan.categories.associateBy {
        it.id.lowercase(Locale.ROOT)
    }
    return plans.mapNotNull { plan ->
        val belongsToActivePlan = plan.studyPlanId.equalsIdentifier(activePlan.planId)
        val belongsToActiveLevel = activePlan.levelId == null ||
            plan.studyPlanLevelId.equalsIdentifier(activePlan.levelId)
        val category = activeCategoriesById[plan.categoryId.lowercase(Locale.ROOT)]
        if (!belongsToActivePlan || !belongsToActiveLevel || category == null) {
            null
        } else {
            plan.copy(categoryName = category.name)
        }
    }
}

internal fun missingActiveCategories(
    plans: List<DailyPlanSummary>,
    activePlan: ActiveStudyPlan
): List<ActiveStudyPlanCategory> {
    val materializedCategoryIds = filterAndEnrichTodayActivities(plans, activePlan)
        .mapTo(mutableSetOf()) { it.categoryId.lowercase(Locale.ROOT) }
    return activePlan.categories.filter { category ->
        category.id.lowercase(Locale.ROOT) !in materializedCategoryIds
    }
}
