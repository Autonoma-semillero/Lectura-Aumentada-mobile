package co.edu.uniautonoma.inclusivereadingar.domain.usecase

import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgress
import co.edu.uniautonoma.inclusivereadingar.domain.repository.LearningRepository

class RegisterProgressUseCase(private val repository: LearningRepository) {
    suspend operator fun invoke(progress: StudentProgress) {
        repository.registerProgress(progress)
    }
}
