package com.example.buggame

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {
    suspend fun insertPlayer(player: PlayerEntity): Long = playerDao.insert(player)

    fun getAllPlayers(): Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    suspend fun getPlayerById(id: Long): PlayerEntity? = playerDao.getPlayerById(id)
}