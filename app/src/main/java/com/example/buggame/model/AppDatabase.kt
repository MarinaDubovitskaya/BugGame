package com.example.buggame.model

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.buggame.model.PlayerDao
import com.example.buggame.model.ScoreDao
import com.example.buggame.model.ScoreEntity

@Database(entities = [PlayerEntity::class, ScoreEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun scoreDao(): ScoreDao
}