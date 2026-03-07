package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class MockLearningRepositoryTest {
    private lateinit var repository: MockLearningRepository

    @Before
    fun setUp() {
        repository = MockLearningRepository()
    }

    @Test
    fun getActiveLearningUnits_returnsSeedData() = runTest {
        val units = repository.getActiveLearningUnits()
        assertThat(units).hasSize(3)
        assertThat(units.first().word).isEqualTo("CASA")
    }

    @Test
    fun registerProgress_storesProgressLog() = runTest {
        val progress = StudentProgress(
            studentId = "student-1",
            learningUnitId = "word-01",
            markerId = "pattern-01",
            success = true,
            timestamp = Instant.parse("2026-03-07T10:00:00Z")
        )

        repository.registerProgress(progress)

        assertThat(repository.getProgressLogs()).hasSize(1)
        assertThat(repository.getProgressLogs().first().learningUnitId).isEqualTo("word-01")
    }
}
