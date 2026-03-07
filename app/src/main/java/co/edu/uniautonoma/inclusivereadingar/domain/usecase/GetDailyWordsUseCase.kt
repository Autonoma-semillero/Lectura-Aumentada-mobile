package co.edu.uniautonoma.inclusivereadingar.domain.usecase

import co.edu.uniautonoma.inclusivereadingar.domain.model.LearningUnit
import co.edu.uniautonoma.inclusivereadingar.domain.repository.LearningRepository

class GetDailyWordsUseCase(private val repository: LearningRepository) {
    suspend operator fun invoke(): List<LearningUnit> = repository.getActiveLearningUnits()
}
