package co.edu.uniautonoma.inclusivereadingar.domain.repository

import co.edu.uniautonoma.inclusivereadingar.domain.model.ArAsset

interface ArAssetRepository {
    suspend fun findByMarker(markerId: String): ArAsset?
}
