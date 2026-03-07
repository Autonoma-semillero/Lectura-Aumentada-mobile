package co.edu.uniautonoma.inclusivereadingar.data.repository

import co.edu.uniautonoma.inclusivereadingar.data.model.LearningUnitDto
import co.edu.uniautonoma.inclusivereadingar.data.model.toDomain
import co.edu.uniautonoma.inclusivereadingar.domain.model.LearningUnit
import co.edu.uniautonoma.inclusivereadingar.domain.model.StudentProgress
import co.edu.uniautonoma.inclusivereadingar.domain.repository.LearningRepository

class MockLearningRepository : LearningRepository {
    private val seedUnits = listOf(
        LearningUnitDto(
            "word-01",
            "CASA",
            "hogar",
            "pattern-01",
            "/assets/models/casa_v1.glb",
            "/assets/audio/casa.mp3",
            "Modelo de una casa roja"
        ),
        LearningUnitDto(
            "word-02",
            "SOL",
            "naturaleza",
            "pattern-02",
            "/assets/models/sol_v1.glb",
            "/assets/audio/sol.mp3",
            "Modelo de un sol amarillo"
        ),
        LearningUnitDto(
            "word-03",
            "GATO",
            "animales",
            "pattern-03",
            "/assets/models/gato_v1.glb",
            "/assets/audio/gato.mp3",
            "Modelo de un gato sentado"
        )
    )

    private val progressLogs = mutableListOf<StudentProgress>()

    override suspend fun getActiveLearningUnits(): List<LearningUnit> = seedUnits.map { it.toDomain() }

    override suspend fun registerProgress(progress: StudentProgress) {
        progressLogs += progress
    }

    fun getProgressLogs(): List<StudentProgress> = progressLogs.toList()
}
