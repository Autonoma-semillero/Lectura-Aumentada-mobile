package co.edu.uniautonoma.inclusivereadingar.data.repository

import android.os.Build
import co.edu.uniautonoma.inclusivereadingar.data.local.SessionStore
import co.edu.uniautonoma.inclusivereadingar.data.remote.DomanPlansApi
import co.edu.uniautonoma.inclusivereadingar.data.remote.DomanSessionsApi
import co.edu.uniautonoma.inclusivereadingar.domain.model.AuthSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DailyPlanSummary
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.DomanSessionHistoryItem
import co.edu.uniautonoma.inclusivereadingar.domain.model.OngoingDomanSession
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgressSummary

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
    private val sessionsApi: DomanSessionsApi
) : DomanSessionDataSource {
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

    suspend fun getTodayPlans(studentId: String): List<DailyPlanSummary> {
        val session = requireSession()
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalDate.now().toString()
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Calendar.getInstance().time)
        }
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

    suspend fun generatePlan(studentId: String, categoryId: String? = null, force: Boolean = true): DailyPlanSummary {
        val session = requireSession()
        return plansApi.generate(studentId, categoryId, force, session.accessToken)
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
