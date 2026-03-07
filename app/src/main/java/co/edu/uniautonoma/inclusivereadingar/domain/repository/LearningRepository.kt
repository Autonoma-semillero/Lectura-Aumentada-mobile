package co.edu.uniautonoma.inclusivereadingar.domain.repository

import co.edu.uniautonoma.inclusivereadingar.domain.model.LearningUnit
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgress

interface LearningRepository {
    suspend fun getActiveLearningUnits(): List<LearningUnit>
    suspend fun registerProgress(progress: StudentProgress)
}
