package com.example.buggame

import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val scoreDao: ScoreDao) {
    suspend fun insertScore(score: ScoreEntity) = scoreDao.insert(score)

    fun getAllScores(): Flow<List<ScoreEntity>> = scoreDao.getAllScores()
}